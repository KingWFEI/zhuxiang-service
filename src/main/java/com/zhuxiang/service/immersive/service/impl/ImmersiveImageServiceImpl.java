package com.zhuxiang.service.immersive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuxiang.service.immersive.common.IdGenerator;
import com.zhuxiang.service.immersive.common.ImmersiveErrors;
import com.zhuxiang.service.immersive.config.ImmersiveAppProperties;
import com.zhuxiang.service.immersive.converter.ImmersiveImageConverter;
import com.zhuxiang.service.immersive.dto.request.SetEntryImageRequest;
import com.zhuxiang.service.immersive.dto.request.SortImagesRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateImmersiveImageRequest;
import com.zhuxiang.service.immersive.dto.response.ImmersiveImageResponse;
import com.zhuxiang.service.immersive.entity.*;
import com.zhuxiang.service.immersive.enums.ProjectionType;
import com.zhuxiang.service.immersive.enums.TourStatus;
import com.zhuxiang.service.immersive.mapper.*;
import com.zhuxiang.service.immersive.service.ImmersiveImageService;
import com.zhuxiang.service.immersive.storage.FileValidationUtil;
import com.zhuxiang.service.service.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImmersiveImageServiceImpl implements ImmersiveImageService {

    private static final Logger log = LoggerFactory.getLogger(ImmersiveImageServiceImpl.class);

    private final ImmersiveTourMapper tourMapper;
    private final ImmersiveSceneMapper sceneMapper;
    private final ImmersiveSceneImageMapper imageMapper;
    private final ImmersiveImageHotspotMapper hotspotMapper;
    private final ImmersiveImageConverter imageConverter;
    private final IdGenerator idGenerator;
    private final ImmersiveAppProperties appProperties;
    private final ObjectStorageService objectStorageService;

    public ImmersiveImageServiceImpl(ImmersiveTourMapper tourMapper, ImmersiveSceneMapper sceneMapper,
                                      ImmersiveSceneImageMapper imageMapper, ImmersiveImageHotspotMapper hotspotMapper,
                                      ImmersiveImageConverter imageConverter, IdGenerator idGenerator,
                                      ImmersiveAppProperties appProperties, ObjectStorageService objectStorageService) {
        this.tourMapper = tourMapper; this.sceneMapper = sceneMapper; this.imageMapper = imageMapper;
        this.hotspotMapper = hotspotMapper; this.imageConverter = imageConverter; this.idGenerator = idGenerator;
        this.appProperties = appProperties; this.objectStorageService = objectStorageService;
    }

    @Override
    public List<ImmersiveImageResponse> uploadImages(String sceneId, List<MultipartFile> files,
                                                      String projectionType, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("上传文件列表不能为空");
        ProjectionType projType = ProjectionType.FLAT;
        if (StringUtils.hasText(projectionType)) {
            try { projType = ProjectionType.fromValue(projectionType.trim().toUpperCase()); }
            catch (IllegalArgumentException e) { throw ImmersiveErrors.badRequest("不支持的投影类型: " + projectionType); }
        }
        ImmersiveSceneEntity scene = sceneMapper.selectById(sceneId);
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        ImmersiveTourEntity tour = validateSceneTourEditable(scene);
        List<ImageMeta> imageMetas = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) throw new IllegalArgumentException("上传文件不能为空: " + file.getOriginalFilename());
            validateImageFile(file);
            imageMetas.add(readImageMeta(file, projType));
        }
        List<String> savedUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                savedUrls.add(saveFile(file, tour.getHouseId(), tour.getId(), sceneId));
            }
        } catch (Exception e) {
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
        boolean hasEntryImage = imageMapper.selectCount(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                .eq(ImmersiveSceneImageEntity::getSceneId, sceneId).eq(ImmersiveSceneImageEntity::getIsEntry, 1)) > 0;
        int maxSort = getMaxSortOrder(sceneId);
        List<ImmersiveImageResponse> results = new ArrayList<>();
        for (int i = 0; i < savedUrls.size(); i++) {
            String imageId = idGenerator.nextImageId();
            boolean isEntry = !hasEntryImage && i == 0;
            ImmersiveSceneImageEntity entity = new ImmersiveSceneImageEntity();
            entity.setId(imageId); entity.setSceneId(sceneId); entity.setImageUrl(savedUrls.get(i));
            entity.setSortOrder(maxSort + i + 1); entity.setIsEntry(isEntry ? 1 : 0); entity.setEnabled(1);
            entity.setProjectionType(projType);
            ImageMeta meta = imageMetas.get(i);
            if (projType == ProjectionType.EQUIRECTANGULAR) { entity.setImageWidth(meta.width); entity.setImageHeight(meta.height); }
            imageMapper.insert(entity);
            if (isEntry) { scene.setEntryImageId(imageId); sceneMapper.updateById(scene); }
            results.add(imageConverter.toResponse(entity));
        }
        return results;
    }

    @Override
    public ImmersiveImageResponse getImage(String imageId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneImageEntity entity = imageMapper.selectById(imageId);
        if (entity == null) throw ImmersiveErrors.imageNotFound();
        return imageConverter.toResponse(entity);
    }

    @Override
    public List<ImmersiveImageResponse> listImages(String sceneId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        return imageMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                .eq(ImmersiveSceneImageEntity::getSceneId, sceneId)
                .orderByAsc(ImmersiveSceneImageEntity::getSortOrder).orderByAsc(ImmersiveSceneImageEntity::getCreatedAt))
                .stream().map(imageConverter::toResponse).collect(Collectors.toList());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void sortImages(String sceneId, SortImagesRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneEntity scene = sceneMapper.selectById(sceneId);
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        validateSceneTourEditable(scene);
        List<String> imageIds = request.getImageIds();
        if (new HashSet<>(imageIds).size() != imageIds.size()) throw ImmersiveErrors.badRequest("图片ID列表包含重复项");
        List<ImmersiveSceneImageEntity> existing = imageMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                .eq(ImmersiveSceneImageEntity::getSceneId, sceneId));
        if (existing.size() != imageIds.size()) throw ImmersiveErrors.badRequest("排序列表必须包含场景下所有图片");
        Set<String> existingIds = existing.stream().map(ImmersiveSceneImageEntity::getId).collect(Collectors.toSet());
        for (String id : imageIds) if (!existingIds.contains(id)) throw ImmersiveErrors.badRequest("图片 " + id + " 不属于当前场景");
        for (int i = 0; i < imageIds.size(); i++) {
            ImmersiveSceneImageEntity img = new ImmersiveSceneImageEntity(); img.setId(imageIds.get(i)); img.setSortOrder(i + 1); imageMapper.updateById(img);
        }
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void setEntryImage(String sceneId, SetEntryImageRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneEntity scene = sceneMapper.selectById(sceneId);
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        validateSceneTourEditable(scene);
        ImmersiveSceneImageEntity image = imageMapper.selectById(request.getImageId());
        if (image == null) throw ImmersiveErrors.imageNotFound();
        if (!image.getSceneId().equals(sceneId)) throw ImmersiveErrors.badRequest("图片不属于当前场景");
        for (ImmersiveSceneImageEntity img : imageMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                .eq(ImmersiveSceneImageEntity::getSceneId, sceneId).eq(ImmersiveSceneImageEntity::getIsEntry, 1))) {
            img.setIsEntry(0); imageMapper.updateById(img);
        }
        image.setIsEntry(1); imageMapper.updateById(image);
        scene.setEntryImageId(image.getId()); sceneMapper.updateById(scene);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void deleteImage(String imageId, String userId) { deleteImage(imageId, false, userId); }

    @Override @Transactional(rollbackFor = Exception.class)
    public void deleteImage(String imageId, boolean force, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneImageEntity image = imageMapper.selectById(imageId);
        if (image == null) throw ImmersiveErrors.imageNotFound();
        ImmersiveSceneEntity scene = sceneMapper.selectById(image.getSceneId());
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        validateSceneTourEditable(scene);
        if (image.getIsEntry() == 1) {
            long imageCount = imageMapper.selectCount(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                    .eq(ImmersiveSceneImageEntity::getSceneId, scene.getId()));
            boolean isLastImage = imageCount <= 1;
            if (!force && !isLastImage) {
                throw ImmersiveErrors.conflict("入口图片不能直接删除，请先设置其他入口图片或使用强制删除");
            }
            scene.setEntryImageId(null); sceneMapper.updateById(scene);
        }
        Long hotspotCount = hotspotMapper.selectCount(new LambdaQueryWrapper<ImmersiveImageHotspotEntity>()
                .eq(ImmersiveImageHotspotEntity::getSourceImageId, imageId));
        if (hotspotCount > 0) {
            if (!force) throw ImmersiveErrors.conflict("有 " + hotspotCount + " 个热点引用该图片，无法删除");
            hotspotMapper.delete(new LambdaQueryWrapper<ImmersiveImageHotspotEntity>().eq(ImmersiveImageHotspotEntity::getSourceImageId, imageId));
        }
        Long targetRefCount = hotspotMapper.selectCount(new LambdaQueryWrapper<ImmersiveImageHotspotEntity>()
                .eq(ImmersiveImageHotspotEntity::getTargetImageId, imageId));
        if (targetRefCount > 0) {
            if (!force) throw ImmersiveErrors.conflict("有 " + targetRefCount + " 个热点的跳转目标指向该图片，无法删除");
            for (ImmersiveImageHotspotEntity ref : hotspotMapper.selectList(new LambdaQueryWrapper<ImmersiveImageHotspotEntity>()
                    .eq(ImmersiveImageHotspotEntity::getTargetImageId, imageId))) {
                ref.setTargetImageId(null); hotspotMapper.updateById(ref);
            }
        }
        imageMapper.deleteById(imageId);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void updateImage(String imageId, UpdateImmersiveImageRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneImageEntity image = imageMapper.selectById(imageId);
        if (image == null) throw ImmersiveErrors.imageNotFound();
        ImmersiveSceneEntity scene = sceneMapper.selectById(image.getSceneId());
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        validateSceneTourEditable(scene);
        String name = request.getName();
        if (name != null) { name = name.trim(); if (name.isEmpty()) name = null; }
        image.setName(name); imageMapper.updateById(image);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public String uploadFloorPlan(String tourId, MultipartFile file, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity tour = tourMapper.selectById(tourId);
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能修改户型图");
        validateImageFile(file);
        String savedUrl = saveFile(file, tour.getHouseId(), tourId, "floor-plan");
        tour.setFloorPlanUrl(savedUrl); tour.setUpdatedBy(userId); tourMapper.updateById(tour);
        return savedUrl;
    }

    // --- helpers ---
    private static class ImageMeta { final int width, height; ImageMeta(int w, int h) { width = w; height = h; } }

    private String saveFile(MultipartFile file, String houseId, String tourId, String subPath) {
        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        String objectKey = "immersive/" + houseId + "/" + tourId + "/" + subPath + "/" + UUID.randomUUID() + ext;
        try (java.io.InputStream is = file.getInputStream()) {
            return objectStorageService.store(objectKey, is, file.getSize(), file.getContentType());
        } catch (java.io.IOException e) {
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }

    private ImageMeta readImageMeta(MultipartFile file, ProjectionType projType) {
        if (projType != ProjectionType.EQUIRECTANGULAR) return new ImageMeta(0, 0);
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) throw ImmersiveErrors.badRequest("无法解析图片: " + file.getOriginalFilename());
            int w = image.getWidth(), h = image.getHeight();
            if (h == 0 || Math.abs((double) w / h - 2.0) > 0.5)
                throw ImmersiveErrors.badRequest("全景图片宽高比不符合等距柱状投影要求（期望约 2:1，实际 " + w + ":" + h + "）");
            return new ImageMeta(w, h);
        } catch (com.zhuxiang.service.common.BusinessException e) { throw e; }
        catch (IOException e) { throw new RuntimeException("读取图片尺寸失败: " + e.getMessage()); }
    }

    private ImmersiveTourEntity validateSceneTourEditable(ImmersiveSceneEntity scene) {
        ImmersiveTourEntity tour = tourMapper.selectById(scene.getTourId());
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不允许修改图片");
        return tour;
    }

    private void validateImageFile(MultipartFile file) {
        try { FileValidationUtil.validate(file.getOriginalFilename(), file.getContentType(), file.getBytes()); }
        catch (com.zhuxiang.service.common.BusinessException e) { throw e; }
        catch (IllegalArgumentException e) { throw ImmersiveErrors.badRequest(e.getMessage()); }
        catch (Exception e) { throw new RuntimeException("文件读取失败"); }
    }

    private int getMaxSortOrder(String sceneId) {
        List<ImmersiveSceneImageEntity> existing = imageMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                .eq(ImmersiveSceneImageEntity::getSceneId, sceneId).orderByDesc(ImmersiveSceneImageEntity::getSortOrder).last("LIMIT 1"));
        return existing.isEmpty() ? 0 : existing.get(0).getSortOrder();
    }
}
