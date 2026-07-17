package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.CommunityDtos;
import com.zhuxiang.service.entity.Community;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.mapper.CommunityMapper;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.service.CommunityService;
import com.zhuxiang.service.service.LocationService;
import com.zhuxiang.service.service.LocationService.PoiItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CommunityServiceImpl extends ServiceImpl<CommunityMapper, Community>
        implements CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityServiceImpl.class);
    private static final double DEDUP_DISTANCE_METERS = 200.0;

    private final LocationService locationService;
    private final HouseMapper houseMapper;

    public CommunityServiceImpl(LocationService locationService, HouseMapper houseMapper) {
        this.locationService = locationService;
        this.houseMapper = houseMapper;
    }

    @Override
    @Cacheable(value = "communitySearch", key = "#keyword + ':' + #cityCode", unless = "#result.items.isEmpty()")
    public CommunityDtos.SearchResponse searchCommunities(String keyword, String cityCode) {
        if (!StringUtils.hasText(keyword) || keyword.trim().length() < 2) {
            return new CommunityDtos.SearchResponse(List.of());
        }
        String kw = keyword.trim();
        List<Community> list = list(
                Wrappers.<Community>lambdaQuery()
                        .like(Community::getName, kw)
                        .eq(Community::getStatus, "approved")
                        .last("LIMIT 20")
        );
        return new CommunityDtos.SearchResponse(list.stream().map(this::toCommunityView).toList());
    }

    @Override
    @Cacheable(value = "poiTextSearch", key = "#keyword + ':' + #cityCode", unless = "#result.items.isEmpty()")
    public CommunityDtos.PoiSearchResponse searchMapPois(String keyword, String cityCode) {
        List<PoiItem> pois = locationService.searchPoisByText(keyword, cityCode);
        return new CommunityDtos.PoiSearchResponse(pois.stream().map(this::toPoiView).toList());
    }

    @Override
    @Cacheable(value = "poiAroundSearch", key = "#keyword + ':' + #longitude + ':' + #latitude + ':' + #radius", unless = "#result.items.isEmpty()")
    public CommunityDtos.PoiSearchResponse searchMapPoisAround(String keyword, BigDecimal longitude,
                                                                BigDecimal latitude, int radius) {
        List<PoiItem> pois = locationService.searchPoisAround(keyword, longitude, latitude, radius);
        return new CommunityDtos.PoiSearchResponse(pois.stream().map(this::toPoiView).toList());
    }

    @Override
    @Transactional
    public CommunityDtos.ImportResponse importFromMap(CommunityDtos.ImportRequest request) {
        // 1. 按 mapProvider + externalPoiId 查重
        Community existing = getOne(
                Wrappers.<Community>lambdaQuery()
                        .eq(Community::getMapProvider, request.mapProvider())
                        .eq(Community::getExternalPoiId, request.externalPoiId())
                        .last("LIMIT 1"),
                false
        );
        if (existing != null) {
            return toImportResponse(existing);
        }

        // 2. 调用高德 POI 详情获取可信数据
        PoiItem detail = locationService.getPoiDetail(request.externalPoiId());
        if (detail == null) {
            throw BusinessException.notFound("未找到该 POI");
        }

        // 3. 按标准化名称 + adCode 再次查重（含坐标距离校验）
        String normalized = normalizeName(detail.name());
        List<Community> nameMatches = list(
                Wrappers.<Community>lambdaQuery()
                        .eq(Community::getNormalizedName, normalized)
                        .eq(Community::getAdCode, detail.adCode())
                        .eq(Community::getStatus, "approved")
                        .last("LIMIT 5")
        );
        for (Community match : nameMatches) {
            double dist = haversineDistance(
                    detail.latitude(), detail.longitude(),
                    match.getLatitude(), match.getLongitude());
            if (dist < DEDUP_DISTANCE_METERS) {
                // 更新该记录的 POI 关联
                match.setMapProvider(request.mapProvider());
                match.setExternalPoiId(request.externalPoiId());
                match.setUpdatedAt(LocalDateTime.now());
                updateById(match);
                return toImportResponse(match);
            }
        }

        // 4. 创建新小区
        Community community = new Community();
        community.setId(UUID.randomUUID().toString());
        community.setName(detail.name());
        community.setNormalizedName(normalized);
        community.setProvince(detail.province());
        community.setCity(detail.city());
        community.setDistrict(detail.district());
        community.setAdCode(detail.adCode());
        community.setAddress(detail.address());
        community.setLongitude(detail.longitude());
        community.setLatitude(detail.latitude());
        community.setCoordinateSystem("GCJ02");
        community.setMapProvider(request.mapProvider());
        community.setExternalPoiId(request.externalPoiId());
        community.setStatus("approved");
        community.setCreatedAt(LocalDateTime.now());
        community.setUpdatedAt(LocalDateTime.now());
        save(community);

        return toImportResponse(community);
    }

    @Override
    @Transactional
    public void mergeCommunities(String sourceId, String targetId) {
        Community source = getById(sourceId);
        if (source == null) throw BusinessException.notFound("源小区不存在");
        Community target = getById(targetId);
        if (target == null) throw BusinessException.notFound("目标小区不存在");
        if (sourceId.equals(targetId)) throw BusinessException.badRequest("不能合并自身");

        List<House> houses = houseMapper.selectList(
                Wrappers.<House>lambdaQuery().eq(House::getCommunityId, sourceId));
        LocalDateTime now = LocalDateTime.now();
        for (House house : houses) {
            house.setCommunityId(targetId);
            house.setUpdatedAt(now);
            houseMapper.updateById(house);
        }

        source.setStatus("merged");
        source.setUpdatedAt(now);
        updateById(source);
    }

    // ── 转换方法 ──

    private CommunityDtos.CommunityView toCommunityView(Community c) {
        return new CommunityDtos.CommunityView(
                c.getId(), c.getName(), c.getProvince(), c.getCity(),
                c.getDistrict(), c.getAddress(), c.getLongitude(), c.getLatitude());
    }

    private CommunityDtos.PoiView toPoiView(PoiItem p) {
        return new CommunityDtos.PoiView(
                p.mapProvider(), p.externalPoiId(), p.name(),
                p.province(), p.city(), p.district(), p.adCode(),
                p.address(), p.longitude(), p.latitude());
    }

    private CommunityDtos.ImportResponse toImportResponse(Community c) {
        return new CommunityDtos.ImportResponse(
                c.getId(), c.getName(), c.getProvince(), c.getCity(),
                c.getDistrict(), c.getAddress(), c.getLongitude(), c.getLatitude());
    }

    // ── 工具方法 ──

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.replace(" ", "").replace("（", "(").replace("）", ")");
    }

    /** Haversine 公式计算两点间距离（米） */
    private double haversineDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return Double.MAX_VALUE;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * 6371000 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
