package com.zhuxiang.service.immersive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhuxiang.service.immersive.common.IdGenerator;
import com.zhuxiang.service.immersive.common.ImmersiveErrors;
import com.zhuxiang.service.immersive.config.ImmersiveAppProperties;
import com.zhuxiang.service.immersive.converter.ImmersiveHotspotConverter;
import com.zhuxiang.service.immersive.dto.request.CreateHotspotRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateHotspotRequest;
import com.zhuxiang.service.immersive.dto.response.ImmersiveHotspotResponse;
import com.zhuxiang.service.immersive.entity.*;
import com.zhuxiang.service.immersive.enums.*;
import com.zhuxiang.service.immersive.mapper.*;
import com.zhuxiang.service.immersive.service.ImmersiveHotspotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImmersiveHotspotServiceImpl implements ImmersiveHotspotService {

    private final ImmersiveTourMapper tourMapper;
    private final ImmersiveSceneMapper sceneMapper;
    private final ImmersiveSceneImageMapper sceneImageMapper;
    private final ImmersiveImageHotspotMapper hotspotMapper;
    private final ImmersiveHotspotConverter hotspotConverter;
    private final IdGenerator idGenerator;
    private final ImmersiveAppProperties appProperties;

    public ImmersiveHotspotServiceImpl(ImmersiveTourMapper tourMapper, ImmersiveSceneMapper sceneMapper,
                                        ImmersiveSceneImageMapper sceneImageMapper, ImmersiveImageHotspotMapper hotspotMapper,
                                        ImmersiveHotspotConverter hotspotConverter, IdGenerator idGenerator,
                                        ImmersiveAppProperties appProperties) {
        this.tourMapper = tourMapper; this.sceneMapper = sceneMapper; this.sceneImageMapper = sceneImageMapper;
        this.hotspotMapper = hotspotMapper; this.hotspotConverter = hotspotConverter;
        this.idGenerator = idGenerator; this.appProperties = appProperties;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public ImmersiveHotspotResponse create(String imageId, CreateHotspotRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveSceneImageEntity sourceImage = sceneImageMapper.selectById(imageId);
        if (sourceImage == null) throw ImmersiveErrors.imageNotFound();
        ImmersiveSceneEntity sourceScene = sceneMapper.selectById(sourceImage.getSceneId());
        if (sourceScene == null) throw ImmersiveErrors.sceneNotFound();
        ImmersiveTourEntity tour = tourMapper.selectById(sourceScene.getTourId());
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能新增热点");

        TargetType targetType = parseTargetType(request.getTargetType());
        String targetSceneId, targetImageId = request.getTargetImageId();
        if (targetType == TargetType.IMAGE) {
            if (!org.springframework.util.StringUtils.hasText(targetImageId))
                throw ImmersiveErrors.badRequest("targetType=IMAGE 时 targetImageId 不能为空");
            ImmersiveSceneImageEntity ti = sceneImageMapper.selectById(targetImageId);
            if (ti == null) throw ImmersiveErrors.notFound("目标图片不存在: " + targetImageId);
            String derivedSceneId = ti.getSceneId();
            if (org.springframework.util.StringUtils.hasText(request.getTargetSceneId())
                    && !request.getTargetSceneId().equals(derivedSceneId))
                throw ImmersiveErrors.badRequest("targetSceneId 与目标图片所属场景不一致");
            targetSceneId = derivedSceneId;
        } else {
            if (!org.springframework.util.StringUtils.hasText(request.getTargetSceneId()))
                throw ImmersiveErrors.badRequest("targetType=SCENE 时 targetSceneId 不能为空");
            targetSceneId = request.getTargetSceneId();
            if (targetSceneId.equals(sourceScene.getId()))
                throw ImmersiveErrors.badRequest("目标房间不能与当前房间相同");
            targetImageId = null;
        }
        ImmersiveSceneEntity targetScene = validateTargetScene(targetSceneId, sourceScene.getTourId());
        validateCoordinates(request.getXRatio(), request.getYRatio(), request.getYaw(), request.getPitch(), sourceScene);

        ImmersiveImageHotspotEntity entity = new ImmersiveImageHotspotEntity();
        entity.setId(idGenerator.nextHotspotId()); entity.setSourceImageId(imageId);
        entity.setLabel(request.getLabel()); entity.setXRatio(request.getXRatio()); entity.setYRatio(request.getYRatio());
        entity.setYaw(request.getYaw()); entity.setPitch(request.getPitch());
        entity.setTargetType(targetType); entity.setTargetSceneId(targetSceneId); entity.setTargetImageId(targetImageId);
        hotspotMapper.insert(entity);

        Map<String, String> entryMap = Collections.singletonMap(targetSceneId, targetScene.getEntryImageId());
        Map<String, ImmersiveSceneEntity> sceneMap = Collections.singletonMap(targetSceneId, targetScene);
        Map<String, ImmersiveSceneImageEntity> imageMap = targetImageId != null
                ? Collections.singletonMap(targetImageId, sceneImageMapper.selectById(targetImageId)) : null;
        return hotspotConverter.toResponse(entity, entryMap, sceneMap, imageMap);
    }

    @Override
    public List<ImmersiveHotspotResponse> listByImage(String imageId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        return listByImages(Collections.singletonList(imageId));
    }

    @Override
    public List<ImmersiveHotspotResponse> listByImages(List<String> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return Collections.emptyList();
        List<ImmersiveImageHotspotEntity> hotspots = hotspotMapper.selectList(
                new LambdaQueryWrapper<ImmersiveImageHotspotEntity>()
                        .in(ImmersiveImageHotspotEntity::getSourceImageId, imageIds)
                        .orderByAsc(ImmersiveImageHotspotEntity::getCreatedAt));
        if (hotspots.isEmpty()) return Collections.emptyList();
        Set<String> targetSceneIds = hotspots.stream().map(ImmersiveImageHotspotEntity::getTargetSceneId).collect(Collectors.toSet());
        Set<String> targetImageIds = hotspots.stream().map(ImmersiveImageHotspotEntity::getTargetImageId)
                .filter(id -> id != null && !id.isEmpty()).collect(Collectors.toSet());
        List<ImmersiveSceneEntity> scenes = sceneMapper.selectList(
                new LambdaQueryWrapper<ImmersiveSceneEntity>().in(ImmersiveSceneEntity::getId, targetSceneIds));
        Map<String, String> entryImageIdByScene = scenes.stream().collect(Collectors.toMap(
                ImmersiveSceneEntity::getId, s -> s.getEntryImageId() != null ? s.getEntryImageId() : "", (a, b) -> a));
        Map<String, ImmersiveSceneEntity> sceneMap = scenes.stream().collect(Collectors.toMap(
                ImmersiveSceneEntity::getId, s -> s, (a, b) -> a));
        Map<String, ImmersiveSceneImageEntity> imageMap = targetImageIds.isEmpty() ? Collections.emptyMap()
                : sceneImageMapper.selectList(new LambdaQueryWrapper<ImmersiveSceneImageEntity>()
                .in(ImmersiveSceneImageEntity::getId, targetImageIds)).stream()
                .collect(Collectors.toMap(ImmersiveSceneImageEntity::getId, i -> i, (a, b) -> a));
        return hotspots.stream().map(h -> hotspotConverter.toResponse(h, entryImageIdByScene, sceneMap, imageMap)).collect(Collectors.toList());
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void update(String hotspotId, UpdateHotspotRequest request, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveImageHotspotEntity hotspot = hotspotMapper.selectById(hotspotId);
        if (hotspot == null) throw ImmersiveErrors.hotspotNotFound();
        ImmersiveTourEntity tour = loadTourByImageId(hotspot.getSourceImageId());
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能修改热点");
        ImmersiveSceneImageEntity sourceImage = sceneImageMapper.selectById(hotspot.getSourceImageId());
        if (sourceImage == null) throw ImmersiveErrors.imageNotFound();
        ImmersiveSceneEntity sourceScene = sceneMapper.selectById(sourceImage.getSceneId());
        if (sourceScene == null) throw ImmersiveErrors.sceneNotFound();

        TargetType targetType = parseTargetType(request.getTargetType());
        String newTargetSceneId, targetImageId = request.getTargetImageId();
        if (targetType == TargetType.IMAGE) {
            if (!org.springframework.util.StringUtils.hasText(targetImageId))
                throw ImmersiveErrors.badRequest("targetType=IMAGE 时 targetImageId 不能为空");
            ImmersiveSceneImageEntity ti = sceneImageMapper.selectById(targetImageId);
            if (ti == null) throw ImmersiveErrors.notFound("目标图片不存在: " + targetImageId);
            String derivedSceneId = ti.getSceneId();
            if (org.springframework.util.StringUtils.hasText(request.getTargetSceneId())
                    && !request.getTargetSceneId().equals(derivedSceneId))
                throw ImmersiveErrors.badRequest("targetSceneId 与目标图片所属场景不一致");
            newTargetSceneId = derivedSceneId;
        } else {
            if (!org.springframework.util.StringUtils.hasText(request.getTargetSceneId()))
                throw ImmersiveErrors.badRequest("targetType=SCENE 时 targetSceneId 不能为空");
            newTargetSceneId = request.getTargetSceneId();
            if (newTargetSceneId.equals(sourceScene.getId()))
                throw ImmersiveErrors.badRequest("目标房间不能与当前房间相同");
            targetImageId = null;
        }
        validateTargetScene(newTargetSceneId, tour.getId());
        validateCoordinates(request.getXRatio(), request.getYRatio(), request.getYaw(), request.getPitch(), sourceScene);

        hotspot.setLabel(request.getLabel()); hotspot.setXRatio(request.getXRatio()); hotspot.setYRatio(request.getYRatio());
        hotspot.setYaw(request.getYaw()); hotspot.setPitch(request.getPitch());
        hotspot.setTargetType(targetType); hotspot.setTargetSceneId(newTargetSceneId); hotspot.setTargetImageId(targetImageId);
        hotspotMapper.updateById(hotspot);
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public void delete(String hotspotId, String userId) {
        if (!appProperties.isEnabled()) throw ImmersiveErrors.featureDisabled();
        ImmersiveImageHotspotEntity hotspot = hotspotMapper.selectById(hotspotId);
        if (hotspot == null) throw ImmersiveErrors.hotspotNotFound();
        ImmersiveTourEntity tour = loadTourByImageId(hotspot.getSourceImageId());
        if (tour.getStatus() == TourStatus.PUBLISHED) throw ImmersiveErrors.tourStatusNotAllowed("已发布项目不能删除热点");
        hotspotMapper.deleteById(hotspotId);
    }

    // --- helpers ---
    private void validateCoordinates(BigDecimal xRatio, BigDecimal yRatio, BigDecimal yaw, BigDecimal pitch, ImmersiveSceneEntity sourceScene) {
        RenderMode renderMode = sourceScene.getRenderMode();
        if (renderMode == null || renderMode == RenderMode.PHOTO) {
            if (xRatio == null || yRatio == null) throw ImmersiveErrors.badRequest("普通图片模式下 xRatio 和 yRatio 必填");
            if (xRatio.compareTo(BigDecimal.ZERO) < 0 || xRatio.compareTo(BigDecimal.ONE) > 0
                    || yRatio.compareTo(BigDecimal.ZERO) < 0 || yRatio.compareTo(BigDecimal.ONE) > 0)
                throw ImmersiveErrors.badRequest("热点坐标非法，xRatio和yRatio必须在0~1之间");
        } else if (renderMode == RenderMode.PANORAMA) {
            if (yaw == null || pitch == null) throw ImmersiveErrors.badRequest("全景模式下 yaw 和 pitch 必填");
            if (yaw.compareTo(BigDecimal.valueOf(-180)) < 0 || yaw.compareTo(BigDecimal.valueOf(180)) > 0
                    || pitch.compareTo(BigDecimal.valueOf(-90)) < 0 || pitch.compareTo(BigDecimal.valueOf(90)) > 0)
                throw ImmersiveErrors.badRequest("全景坐标非法，yaw必须在-180~180之间，pitch必须在-90~90之间");
        }
    }

    private ImmersiveSceneEntity validateTargetScene(String targetSceneId, String tourId) {
        ImmersiveSceneEntity target = sceneMapper.selectById(targetSceneId);
        if (target == null) throw ImmersiveErrors.notFound("目标房间不存在");
        if (!target.getTourId().equals(tourId)) throw ImmersiveErrors.badRequest("热点不能跨项目引用");
        if (target.getEnabled() == 0) throw ImmersiveErrors.badRequest("目标房间已被禁用");
        if (target.getEntryImageId() == null || target.getEntryImageId().isEmpty())
            throw ImmersiveErrors.badRequest("目标房间尚未设置入口图片");
        return target;
    }

    private TargetType parseTargetType(String targetType) {
        if (targetType == null || targetType.isEmpty()) return TargetType.SCENE;
        try { return TargetType.fromValue(targetType); }
        catch (IllegalArgumentException e) { throw ImmersiveErrors.badRequest("不支持的跳转类型: " + targetType); }
    }

    private ImmersiveTourEntity loadTourByImageId(String imageId) {
        ImmersiveSceneImageEntity image = sceneImageMapper.selectById(imageId);
        if (image == null) throw ImmersiveErrors.imageNotFound();
        ImmersiveSceneEntity scene = sceneMapper.selectById(image.getSceneId());
        if (scene == null) throw ImmersiveErrors.sceneNotFound();
        ImmersiveTourEntity tour = tourMapper.selectById(scene.getTourId());
        if (tour == null) throw ImmersiveErrors.tourNotFound();
        return tour;
    }
}
