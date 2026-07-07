package com.zhuxiang.service.immersive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.immersive.common.IdGenerator;
import com.zhuxiang.service.immersive.common.ImmersiveErrors;
import com.zhuxiang.service.immersive.config.ImmersiveAppProperties;
import com.zhuxiang.service.immersive.converter.ImmersiveImageConverter;
import com.zhuxiang.service.immersive.converter.ImmersiveSceneConverter;
import com.zhuxiang.service.immersive.converter.ImmersiveTourConverter;
import com.zhuxiang.service.immersive.dto.request.CreateImmersiveTourRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateImmersiveTourRequest;
import com.zhuxiang.service.immersive.dto.response.AdminImmersiveTourDetailResponse;
import com.zhuxiang.service.immersive.dto.response.AvailabilityResponse;
import com.zhuxiang.service.immersive.dto.response.ImmersiveHotspotResponse;
import com.zhuxiang.service.immersive.dto.response.ImmersiveImageResponse;
import com.zhuxiang.service.immersive.dto.response.ImmersiveSceneResponse;
import com.zhuxiang.service.immersive.dto.response.ImmersiveTourSummaryResponse;
import com.zhuxiang.service.immersive.entity.*;
import com.zhuxiang.service.immersive.enums.*;
import com.zhuxiang.service.immersive.mapper.*;
import com.zhuxiang.service.immersive.service.ImmersiveHotspotService;
import com.zhuxiang.service.immersive.service.ImmersiveTourService;
import com.zhuxiang.service.service.HouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImmersiveTourServiceImpl implements ImmersiveTourService {

    private static final Logger log = LoggerFactory.getLogger(ImmersiveTourServiceImpl.class);

    private final ImmersiveTourMapper tourMapper;
    private final ImmersiveSceneMapper sceneMapper;
    private final ImmersiveSceneImageMapper sceneImageMapper;
    private final ImmersiveImageHotspotMapper hotspotMapper;
    private final ImmersiveTourConverter tourConverter;
    private final ImmersiveSceneConverter sceneConverter;
    private final ImmersiveImageConverter imageConverter;
    private final ImmersiveHotspotService hotspotService;
    private final IdGenerator idGenerator;
    private final ImmersiveAppProperties appProperties;
    private final HouseService houseService;

    public ImmersiveTourServiceImpl(ImmersiveTourMapper tourMapper,
                                     ImmersiveSceneMapper sceneMapper,
                                     ImmersiveSceneImageMapper sceneImageMapper,
                                     ImmersiveImageHotspotMapper hotspotMapper,
                                     ImmersiveTourConverter tourConverter,
                                     ImmersiveSceneConverter sceneConverter,
                                     ImmersiveImageConverter imageConverter,
                                     ImmersiveHotspotService hotspotService,
                                     IdGenerator idGenerator,
                                     ImmersiveAppProperties appProperties,
                                     HouseService houseService) {
        this.tourMapper = tourMapper;
        this.sceneMapper = sceneMapper;
        this.sceneImageMapper = sceneImageMapper;
        this.hotspotMapper = hotspotMapper;
        this.tourConverter = tourConverter;
        this.sceneConverter = sceneConverter;
        this.imageConverter = imageConverter;
        this.hotspotService = hotspotService;
        this.idGenerator = idGenerator;
        this.appProperties = appProperties;
        this.houseService = houseService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImmersiveTourSummaryResponse create(String houseId, CreateImmersiveTourRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();

        // 直接注入 HouseService，替代 HTTP 远程调用
        House house = houseService.getById(houseId);
        if (house == null) throw ImmersiveErrors.houseNotFound();

        Long count = tourMapper.selectCount(
                new LambdaQueryWrapper<ImmersiveTourEntity>().eq(ImmersiveTourEntity::getHouseId, houseId));
        if (count > 0) throw ImmersiveErrors.houseAlreadyHasTour();

        ImmersiveTourEntity entity = new ImmersiveTourEntity();
        entity.setId(idGenerator.nextTourId());
        entity.setHouseId(houseId);
        entity.setTitle(request.getTitle());
        entity.setStatus(TourStatus.DRAFT);
        entity.setActiveKey(houseId);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        try {
            tourMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw ImmersiveErrors.houseAlreadyHasTour();
        }
        return tourConverter.toSummary(entity);
    }

    @Override
    public ImmersiveTourSummaryResponse getByHouseId(String houseId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity entity = tourMapper.selectOne(
                new LambdaQueryWrapper<ImmersiveTourEntity>().eq(ImmersiveTourEntity::getHouseId, houseId));
        return tourConverter.toSummary(entity);
    }

    @Override
    public AdminImmersiveTourDetailResponse getDetail(String tourId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity entity = tourMapper.selectById(tourId);
        if (entity == null) throw ImmersiveErrors.tourNotFound();
        return buildDetailResponse(entity, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String tourId, UpdateImmersiveTourRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity entity = tourMapper.selectById(tourId);
        if (entity == null) throw ImmersiveErrors.tourNotFound();
        validateEditable(entity);
        boolean changed = false;
        if (StringUtils.hasText(request.getTitle())) { entity.setTitle(request.getTitle()); changed = true; }
        if (request.getCoverImageUrl() != null) { entity.setCoverImageUrl(request.getCoverImageUrl()); changed = true; }
        if (changed) { entity.setUpdatedBy(userId); tourMapper.updateById(entity); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String tourId, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity entity = tourMapper.selectById(tourId);
        if (entity == null) throw ImmersiveErrors.tourNotFound();
        if (entity.getStatus() == TourStatus.PUBLISHED)
            throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能删除，请先下线");
        Long sceneCount = sceneMapper.selectCount(
                new LambdaQueryWrapper<ImmersiveSceneEntity>().eq(ImmersiveSceneEntity::getTourId, tourId));
        if (sceneCount > 0)
            throw ImmersiveErrors.tourStatusNotAllowed("项目下还有未删除的房间，请先删除所有房间");
        entity.setActiveKey(null);
        entity.setUpdatedBy(userId);
        tourMapper.updateById(entity);
        tourMapper.deleteById(tourId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImmersiveTourSummaryResponse publish(String tourId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity tour = tourMapper.selectById(tourId);
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.OFFLINE)
            throw ImmersiveErrors.tourStatusNotAllowed("只有草稿或下线状态的项目才能发布，当前状态：" + tour.getStatus().getLabel());

        List<ImmersiveSceneEntity> enabledScenes = loadEnabledScenes(tourId);
        List<ImmersiveSceneImageEntity> enabledImages = loadEnabledImages(tourId);
        List<ImmersiveImageHotspotEntity> hotspots = loadHotspots(enabledImages);

        Map<String, ImmersiveSceneEntity> sceneMap = enabledScenes.stream()
                .collect(Collectors.toMap(ImmersiveSceneEntity::getId, s -> s));
        Map<String, ImmersiveSceneImageEntity> imageMap = enabledImages.stream()
                .collect(Collectors.toMap(ImmersiveSceneImageEntity::getId, i -> i));
        Map<String, List<ImmersiveSceneImageEntity>> imagesByScene = enabledImages.stream()
                .collect(Collectors.groupingBy(ImmersiveSceneImageEntity::getSceneId));

        validatePublish(tour, enabledScenes, enabledImages, hotspots, sceneMap, imageMap, imagesByScene);

        tour.setStatus(TourStatus.PUBLISHED);
        tour.setPublishedAt(LocalDateTime.now());
        tour.setUpdatedBy("system");
        tourMapper.updateById(tour);
        return tourConverter.toSummary(tour);
    }

    private void validatePublish(ImmersiveTourEntity tour,
            List<ImmersiveSceneEntity> enabledScenes,
            List<ImmersiveSceneImageEntity> enabledImages,
            List<ImmersiveImageHotspotEntity> hotspots,
            Map<String, ImmersiveSceneEntity> sceneMap,
            Map<String, ImmersiveSceneImageEntity> imageMap,
            Map<String, List<ImmersiveSceneImageEntity>> imagesByScene) {

        if (!StringUtils.hasText(tour.getEntrySceneId()))
            throw ImmersiveErrors.entrySceneNotSet();

        ImmersiveSceneEntity entryScene = sceneMap.get(tour.getEntrySceneId());
        if (entryScene == null)
            throw ImmersiveErrors.publishValidationFailed("入口房间不存在或已禁用");
        if (!StringUtils.hasText(entryScene.getEntryImageId()))
            throw ImmersiveErrors.publishValidationFailed("入口房间未设置入口图片");

        ImmersiveSceneImageEntity entryImage = imageMap.get(entryScene.getEntryImageId());
        if (entryImage == null)
            throw ImmersiveErrors.publishValidationFailed("入口房间的入口图片不存在或已禁用");
        if (!entryImage.getSceneId().equals(entryScene.getId()))
            throw ImmersiveErrors.publishValidationFailed("入口房间的入口图片不属于该房间");

        if (enabledScenes.isEmpty())
            throw ImmersiveErrors.publishValidationFailed("至少需要一个启用房间");

        for (ImmersiveSceneEntity scene : enabledScenes) {
            List<ImmersiveSceneImageEntity> sceneImages = imagesByScene.getOrDefault(scene.getId(), List.of());
            if (sceneImages.isEmpty())
                throw ImmersiveErrors.publishValidationFailed("房间「" + scene.getName() + "」未上传任何图片");
            if (!StringUtils.hasText(scene.getEntryImageId()))
                throw ImmersiveErrors.publishValidationFailed("房间「" + scene.getName() + "」未设置入口图片");
            ImmersiveSceneImageEntity sceneEntryImage = imageMap.get(scene.getEntryImageId());
            if (sceneEntryImage == null)
                throw ImmersiveErrors.publishValidationFailed("房间「" + scene.getName() + "」的入口图片不存在或已禁用");
            if (!sceneEntryImage.getSceneId().equals(scene.getId()))
                throw ImmersiveErrors.publishValidationFailed("房间「" + scene.getName() + "」的入口图片不属于该房间");

            RenderMode renderMode = scene.getRenderMode();
            if (renderMode == null || renderMode == RenderMode.PHOTO) {
                if (sceneEntryImage.getProjectionType() != null && sceneEntryImage.getProjectionType() != ProjectionType.FLAT)
                    throw ImmersiveErrors.publishValidationFailed("房间「" + scene.getName() + "」为 PHOTO 模式，入口图片必须为 FLAT 投影类型");
            } else if (renderMode == RenderMode.PANORAMA) {
                if (sceneEntryImage.getProjectionType() == null || sceneEntryImage.getProjectionType() != ProjectionType.EQUIRECTANGULAR)
                    throw ImmersiveErrors.publishValidationFailed("房间「" + scene.getName() + "」为 PANORAMA 模式，入口图片必须为 EQUIRECTANGULAR 投影类型");
            }
        }

        for (ImmersiveImageHotspotEntity hotspot : hotspots) {
            ImmersiveSceneImageEntity sourceImage = imageMap.get(hotspot.getSourceImageId());
            if (sourceImage == null) throw ImmersiveErrors.publishValidationFailed("热点源图片不存在或已禁用");
            ImmersiveSceneEntity sourceScene = sceneMap.get(sourceImage.getSceneId());
            RenderMode renderMode = sourceScene != null ? sourceScene.getRenderMode() : null;
            if (renderMode == null || renderMode == RenderMode.PHOTO) {
                BigDecimal xRatio = hotspot.getXRatio();
                BigDecimal yRatio = hotspot.getYRatio();
                if (xRatio == null || xRatio.compareTo(BigDecimal.ZERO) < 0 || xRatio.compareTo(BigDecimal.ONE) > 0
                        || yRatio == null || yRatio.compareTo(BigDecimal.ZERO) < 0 || yRatio.compareTo(BigDecimal.ONE) > 0)
                    throw ImmersiveErrors.publishValidationFailed("热点坐标非法，xRatio 和 yRatio 必须在 0~1 之间");
            } else if (renderMode == RenderMode.PANORAMA) {
                BigDecimal yaw = hotspot.getYaw();
                BigDecimal pitch = hotspot.getPitch();
                if (yaw == null || pitch == null)
                    throw ImmersiveErrors.publishValidationFailed("全景热点缺少 yaw/pitch 坐标");
                if (yaw.compareTo(BigDecimal.valueOf(-180)) < 0 || yaw.compareTo(BigDecimal.valueOf(180)) > 0
                        || pitch.compareTo(BigDecimal.valueOf(-90)) < 0 || pitch.compareTo(BigDecimal.valueOf(90)) > 0)
                    throw ImmersiveErrors.publishValidationFailed("全景热点坐标非法");
            }

            TargetType targetType = hotspot.getTargetType();
            if (targetType == TargetType.IMAGE) {
                if (!StringUtils.hasText(hotspot.getTargetImageId()))
                    throw ImmersiveErrors.publishValidationFailed("IMAGE 类型热点缺少 targetImageId");
                ImmersiveSceneImageEntity targetImage = imageMap.get(hotspot.getTargetImageId());
                if (targetImage == null)
                    throw ImmersiveErrors.publishValidationFailed("IMAGE 类型热点的目标图片不存在或已禁用");
                if (!hotspot.getTargetSceneId().equals(targetImage.getSceneId()))
                    throw ImmersiveErrors.publishValidationFailed("IMAGE 类型热点的 targetSceneId 与目标图片所属场景不一致");
            }
            ImmersiveSceneEntity targetScene = sceneMap.get(hotspot.getTargetSceneId());
            if (targetScene == null) throw ImmersiveErrors.publishValidationFailed("热点目标房间不存在或已禁用");
            if (!targetScene.getTourId().equals(tour.getId())) throw ImmersiveErrors.publishValidationFailed("热点不能跨项目引用");
            if (!StringUtils.hasText(targetScene.getEntryImageId()))
                throw ImmersiveErrors.publishValidationFailed("热点目标房间「" + targetScene.getName() + "」未设置入口图片");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImmersiveTourSummaryResponse offline(String tourId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveTourEntity tour = tourMapper.selectById(tourId);
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() != TourStatus.PUBLISHED)
            throw ImmersiveErrors.tourStatusNotAllowed("只有已发布的项目才能下线，当前状态：" + tour.getStatus().getLabel());
        tour.setStatus(TourStatus.OFFLINE);
        tour.setUpdatedBy("system");
        tourMapper.updateById(tour);
        return tourConverter.toSummary(tour);
    }

    @Override
    public AvailabilityResponse getAvailability(String houseId) {
        AvailabilityResponse resp = new AvailabilityResponse();
        resp.setAvailable(false);
        if (!appProperties.isEnabled()) return resp;
        if (!isHouseVisible(houseId)) return resp;
        ImmersiveTourEntity tour = findPublishedTour(houseId);
        if (tour == null) return resp;
        String coverImageUrl = resolveCoverImageUrl(tour);
        resp.setAvailable(true);
        resp.setTourId(tour.getId());
        resp.setCoverImageUrl(coverImageUrl);
        return resp;
    }

    @Override
    public AdminImmersiveTourDetailResponse getUserTourData(String houseId) {
        if (!appProperties.isEnabled()) return null;
        if (!isHouseVisible(houseId)) return null;
        ImmersiveTourEntity tour = findPublishedTour(houseId);
        if (tour == null) return null;
        return buildDetailResponse(tour, true);
    }

    public void validateEditable(ImmersiveTourEntity entity) {
        if (entity.getStatus() == TourStatus.PUBLISHED)
            throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不允许编辑");
    }

    // ============ 房源可见性判断，与原项目 GET /api/houses/{houseId} 一致 ============
    private boolean isHouseVisible(String houseId) {
        House house = houseService.getById(houseId);
        if (house == null) return false;
        return "available".equals(house.getStatus()) || "reserved".equals(house.getStatus());
    }

    private ImmersiveTourEntity findPublishedTour(String houseId) {
        List<ImmersiveTourEntity> tours = tourMapper.selectList(
                new LambdaQueryWrapper<ImmersiveTourEntity>()
                        .eq(ImmersiveTourEntity::getHouseId, houseId)
                        .eq(ImmersiveTourEntity::getStatus, TourStatus.PUBLISHED));
        return tours.isEmpty() ? null : tours.get(0);
    }

    private String resolveCoverImageUrl(ImmersiveTourEntity tour) {
        if (!StringUtils.hasText(tour.getEntrySceneId())) return null;
        ImmersiveSceneEntity entryScene = sceneMapper.selectById(tour.getEntrySceneId());
        if (entryScene == null || !StringUtils.hasText(entryScene.getEntryImageId())) return null;
        ImmersiveSceneImageEntity entryImage = sceneImageMapper.selectById(entryScene.getEntryImageId());
        return entryImage != null ? entryImage.getImageUrl() : null;
    }

    private AdminImmersiveTourDetailResponse buildDetailResponse(ImmersiveTourEntity tour, boolean userView) {
        List<ImmersiveSceneEntity> scenes = sceneMapper.selectList(
                new LambdaQueryWrapper<ImmersiveSceneEntity>()
                        .eq(ImmersiveSceneEntity::getTourId, tour.getId())
                        .eq(userView, ImmersiveSceneEntity::getEnabled, 1)
                        .orderByAsc(ImmersiveSceneEntity::getSortOrder)
                        .orderByAsc(ImmersiveSceneEntity::getCreatedAt));

        List<ImmersiveSceneResponse> sceneResponses = new ArrayList<>();
        List<String> allImageIds = new ArrayList<>();
        for (ImmersiveSceneEntity scene : scenes) {
            ImmersiveSceneResponse sceneResp = sceneConverter.toResponse(scene);
            LambdaQueryWrapper<ImmersiveSceneImageEntity> imageQuery =
                    new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                            .eq(ImmersiveSceneImageEntity::getSceneId, scene.getId())
                            .orderByAsc(ImmersiveSceneImageEntity::getSortOrder)
                            .orderByAsc(ImmersiveSceneImageEntity::getCreatedAt);
            if (userView) imageQuery.eq(ImmersiveSceneImageEntity::getEnabled, 1);
            List<ImmersiveImageResponse> imageResponses = sceneImageMapper.selectList(imageQuery)
                    .stream().map(imageConverter::toResponse).collect(Collectors.toList());
            allImageIds.addAll(imageResponses.stream().map(ImmersiveImageResponse::getImageId).toList());
            sceneResp.setImages(imageResponses);
            sceneResponses.add(sceneResp);
        }

        if (!allImageIds.isEmpty()) {
            List<ImmersiveHotspotResponse> allHotspots = hotspotService.listByImages(allImageIds);
            Map<String, List<ImmersiveHotspotResponse>> hotspotsByImage = allHotspots.stream()
                    .collect(Collectors.groupingBy(ImmersiveHotspotResponse::getSourceImageId));
            for (ImmersiveSceneResponse sceneResp : sceneResponses) {
                for (ImmersiveImageResponse img : sceneResp.getImages()) {
                    if (userView && (img.getEntry() == null || !img.getEntry())) {
                        img.setHotspots(List.of());
                    } else {
                        img.setHotspots(hotspotsByImage.getOrDefault(img.getImageId(), List.of()));
                    }
                }
            }
        }
        return tourConverter.toAdminDetail(tour, sceneResponses);
    }

    private List<ImmersiveSceneEntity> loadEnabledScenes(String tourId) {
        return sceneMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneEntity>()
                .eq(ImmersiveSceneEntity::getTourId, tourId).eq(ImmersiveSceneEntity::getEnabled, 1));
    }

    private List<ImmersiveSceneImageEntity> loadEnabledImages(String tourId) {
        List<ImmersiveSceneEntity> scenes = loadEnabledScenes(tourId);
        if (scenes.isEmpty()) return List.of();
        List<String> sceneIds = scenes.stream().map(ImmersiveSceneEntity::getId).toList();
        return sceneImageMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                .in(ImmersiveSceneImageEntity::getSceneId, sceneIds).eq(ImmersiveSceneImageEntity::getEnabled, 1));
    }

    private List<ImmersiveImageHotspotEntity> loadHotspots(List<ImmersiveSceneImageEntity> images) {
        if (images.isEmpty()) return List.of();
        List<String> imageIds = images.stream().map(ImmersiveSceneImageEntity::getId).toList();
        return hotspotMapper.selectList(new LambdaQueryWrapper<ImmersiveImageHotspotEntity>()
                .in(ImmersiveImageHotspotEntity::getSourceImageId, imageIds));
    }
}
