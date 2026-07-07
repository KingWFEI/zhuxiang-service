package com.zhuxiang.service.immersive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuxiang.service.immersive.common.IdGenerator;
import com.zhuxiang.service.immersive.common.ImmersiveErrors;
import com.zhuxiang.service.immersive.config.ImmersiveAppProperties;
import com.zhuxiang.service.immersive.converter.ImmersiveSceneConverter;
import com.zhuxiang.service.immersive.dto.request.*;
import com.zhuxiang.service.immersive.dto.response.ImmersiveSceneResponse;
import com.zhuxiang.service.immersive.entity.*;
import com.zhuxiang.service.immersive.enums.TourStatus;
import com.zhuxiang.service.immersive.mapper.*;
import com.zhuxiang.service.immersive.service.ImmersiveSceneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImmersiveSceneServiceImpl implements ImmersiveSceneService {

    private static final Logger log = LoggerFactory.getLogger(ImmersiveSceneServiceImpl.class);

    private final ImmersiveTourMapper tourMapper;
    private final ImmersiveSceneMapper sceneMapper;
    private final ImmersiveSceneImageMapper sceneImageMapper;
    private final ImmersiveImageHotspotMapper hotspotMapper;
    private final ImmersiveSceneConverter sceneConverter;
    private final IdGenerator idGenerator;
    private final ImmersiveAppProperties appProperties;

    public ImmersiveSceneServiceImpl(ImmersiveTourMapper tourMapper, ImmersiveSceneMapper sceneMapper,
                                      ImmersiveSceneImageMapper sceneImageMapper, ImmersiveImageHotspotMapper hotspotMapper,
                                      ImmersiveSceneConverter sceneConverter, IdGenerator idGenerator,
                                      ImmersiveAppProperties appProperties) {
        this.tourMapper = tourMapper; this.sceneMapper = sceneMapper; this.sceneImageMapper = sceneImageMapper;
        this.hotspotMapper = hotspotMapper; this.sceneConverter = sceneConverter;
        this.idGenerator = idGenerator; this.appProperties = appProperties;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public ImmersiveSceneResponse create(String tourId, CreateImmersiveSceneRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity tour = tourMapper.selectById(tourId);
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能新增房间");
        Long nameCount = sceneMapper.selectCount(new LambdaQueryWrapper<ImmersiveSceneEntity>()
                .eq(ImmersiveSceneEntity::getTourId, tourId).eq(ImmersiveSceneEntity::getName, request.getName()));
        if (nameCount > 0) throw ImmersiveErrors.badRequest("同一项目下房间名称不可重复");
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            List<ImmersiveSceneEntity> all = sceneMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneEntity>()
                    .eq(ImmersiveSceneEntity::getTourId, tourId).orderByDesc(ImmersiveSceneEntity::getSortOrder).last("LIMIT 1"));
            sortOrder = all.isEmpty() ? 0 : all.get(0).getSortOrder() + 1;
        }
        ImmersiveSceneEntity entity = new ImmersiveSceneEntity();
        entity.setId(idGenerator.nextSceneId());
        entity.setTourId(tourId);
        entity.setName(request.getName());
        entity.setSceneType(request.getSceneType());
        entity.setRenderMode(request.getRenderMode() != null ? request.getRenderMode() : com.zhuxiang.service.immersive.enums.RenderMode.PHOTO);
        entity.setInitialYaw(request.getInitialYaw());
        entity.setInitialPitch(request.getInitialPitch());
        entity.setInitialHfov(request.getInitialHfov());
        entity.setSortOrder(sortOrder);
        entity.setEnabled(1);
        sceneMapper.insert(entity);
        return sceneConverter.toResponse(entity);
    }

    @Override
    public List<ImmersiveSceneResponse> listByTourId(String tourId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        return sceneMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneEntity>()
                .eq(ImmersiveSceneEntity::getTourId, tourId)
                .orderByAsc(ImmersiveSceneEntity::getSortOrder).orderByAsc(ImmersiveSceneEntity::getCreatedAt))
                .stream().map(sceneConverter::toResponse).collect(Collectors.toList());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void update(String sceneId, UpdateImmersiveSceneRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneEntity scene = sceneMapper.selectById(sceneId);
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        ImmersiveTourEntity tour = tourMapper.selectById(scene.getTourId());
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能修改房间");
        boolean changed = false;
        if (StringUtils.hasText(request.getName())) {
            Long nameCount = sceneMapper.selectCount(new LambdaQueryWrapper<ImmersiveSceneEntity>()
                    .eq(ImmersiveSceneEntity::getTourId, scene.getTourId()).eq(ImmersiveSceneEntity::getName, request.getName())
                    .ne(ImmersiveSceneEntity::getId, sceneId));
            if (nameCount > 0) throw ImmersiveErrors.badRequest("同一项目下房间名称不可重复");
            scene.setName(request.getName()); changed = true;
        }
        if (request.getSceneType() != null) { scene.setSceneType(request.getSceneType()); changed = true; }
        if (request.getRenderMode() != null) { scene.setRenderMode(request.getRenderMode()); changed = true; }
        if (request.getInitialYaw() != null) { scene.setInitialYaw(request.getInitialYaw()); changed = true; }
        if (request.getInitialPitch() != null) { scene.setInitialPitch(request.getInitialPitch()); changed = true; }
        if (request.getInitialHfov() != null) { scene.setInitialHfov(request.getInitialHfov()); changed = true; }
        if (request.getEnabled() != null) { scene.setEnabled(request.getEnabled()); changed = true; }
        if (changed) sceneMapper.updateById(scene);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void delete(String sceneId, String userId) { delete(sceneId, false, userId); }

    @Override @Transactional(rollbackFor = Exception.class)
    public void delete(String sceneId, boolean cascade, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneEntity scene = sceneMapper.selectById(sceneId);
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        ImmersiveTourEntity tour = tourMapper.selectById(scene.getTourId());
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能删除房间");
        if (sceneId.equals(tour.getEntrySceneId()))
            throw ImmersiveErrors.conflict("该房间是项目入口房间，请先设置其他入口房间后再删除");
        if (!cascade) {
            Long imageCount = sceneImageMapper.selectCount(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                    .eq(ImmersiveSceneImageEntity::getSceneId, sceneId));
            if (imageCount > 0) throw ImmersiveErrors.conflict("房间下还有 " + imageCount + " 张图片，请先删除图片或使用级联删除");
            Long hotspotCount = hotspotMapper.selectCount(new LambdaQueryWrapper<ImmersiveImageHotspotEntity>()
                    .eq(ImmersiveImageHotspotEntity::getTargetSceneId, sceneId));
            if (hotspotCount > 0) throw ImmersiveErrors.conflict("有 " + hotspotCount + " 个热点指向该房间，无法删除");
            sceneMapper.deleteById(sceneId);
            return;
        }
        List<ImmersiveSceneImageEntity> images = sceneImageMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                .eq(ImmersiveSceneImageEntity::getSceneId, sceneId));
        List<String> imageIds = images.stream().map(ImmersiveSceneImageEntity::getId).collect(Collectors.toList());
        if (!imageIds.isEmpty()) hotspotMapper.delete(new LambdaQueryWrapper<ImmersiveImageHotspotEntity>()
                .in(ImmersiveImageHotspotEntity::getSourceImageId, imageIds));
        hotspotMapper.delete(new LambdaQueryWrapper<ImmersiveImageHotspotEntity>()
                .eq(ImmersiveImageHotspotEntity::getTargetSceneId, sceneId));
        for (ImmersiveSceneImageEntity img : images) {
            sceneImageMapper.deleteById(img.getId());
            if (StringUtils.hasText(img.getImageUrl())) {
                try { java.io.File f = new java.io.File(img.getImageUrl()); if (f.exists()) f.delete(); }
                catch (Exception ignored) { log.warn("删除图片文件失败: {}", img.getImageUrl()); }
            }
        }
        sceneMapper.deleteById(sceneId);
        log.info("级联删除场景完成: sceneId={}, 删除图片 {} 张", sceneId, images.size());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void sort(String tourId, SortImmersiveScenesRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity tour = tourMapper.selectById(tourId);
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能排序房间");
        List<String> sceneIds = request.getSceneIds();
        if (new HashSet<>(sceneIds).size() != sceneIds.size()) throw ImmersiveErrors.badRequest("场景ID列表包含重复项");
        List<ImmersiveSceneEntity> existing = sceneMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneEntity>()
                .eq(ImmersiveSceneEntity::getTourId, tourId));
        if (existing.size() != sceneIds.size()) throw ImmersiveErrors.badRequest("排序列表必须包含项目下所有场景");
        Set<String> existingIds = existing.stream().map(ImmersiveSceneEntity::getId).collect(Collectors.toSet());
        for (String id : sceneIds) if (!existingIds.contains(id)) throw ImmersiveErrors.badRequest("场景 " + id + " 不属于当前项目");
        for (int i = 0; i < sceneIds.size(); i++) {
            ImmersiveSceneEntity s = new ImmersiveSceneEntity(); s.setId(sceneIds.get(i)); s.setSortOrder(i + 1); sceneMapper.updateById(s);
        }
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void setEntryScene(String tourId, SetEntrySceneRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity tour = tourMapper.selectById(tourId);
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能修改入口房间");
        ImmersiveSceneEntity scene = sceneMapper.selectById(request.getSceneId());
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        if (!scene.getTourId().equals(tourId)) throw ImmersiveErrors.badRequest("入口房间不属于当前项目");
        if (scene.getEnabled() == 0) throw ImmersiveErrors.badRequest("入口房间已被禁用");
        tour.setEntrySceneId(request.getSceneId());
        tour.setUpdatedBy(userId);
        tourMapper.updateById(tour);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void setFloorPlanPosition(String sceneId, SetSceneFloorPlanPositionRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneEntity scene = sceneMapper.selectById(sceneId);
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        ImmersiveTourEntity tour = tourMapper.selectById(scene.getTourId());
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能修改户型图位置");
        BigDecimal x = request.getXRatio(), y = request.getYRatio();
        if (x == null || y == null || x.compareTo(BigDecimal.ZERO) < 0 || x.compareTo(BigDecimal.ONE) > 0
                || y.compareTo(BigDecimal.ZERO) < 0 || y.compareTo(BigDecimal.ONE) > 0)
            throw ImmersiveErrors.badRequest("热点坐标非法，xRatio和yRatio必须在0~1之间");
        scene.setFloorPlanXRatio(x); scene.setFloorPlanYRatio(y); sceneMapper.updateById(scene);
    }
}
