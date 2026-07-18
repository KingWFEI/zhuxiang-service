package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class CommunityDtos {

    private CommunityDtos() {}

    // ── 通用 POI / 小区视图 ──

    @Schema(description = "小区/POI 基础信息")
    public record CommunityView(
            @Schema(description = "内部小区 ID（来自地图 POI 时为空）") String id,
            @Schema(description = "名称") String name,
            @Schema(description = "省") String province,
            @Schema(description = "市") String city,
            @Schema(description = "区") String district,
            @Schema(description = "地址") String address,
            @Schema(description = "经度") BigDecimal longitude,
            @Schema(description = "纬度") BigDecimal latitude
    ) {}

    @Schema(description = "地图 POI 视图（区别于内部小区，多了 mapProvider / externalPoiId）")
    public record PoiView(
            @Schema(description = "地图供应商") String mapProvider,
            @Schema(description = "地图平台 POI ID") String externalPoiId,
            @Schema(description = "名称") String name,
            @Schema(description = "省") String province,
            @Schema(description = "市") String city,
            @Schema(description = "区") String district,
            @Schema(description = "行政区划代码") String adCode,
            @Schema(description = "地址") String address,
            @Schema(description = "经度") BigDecimal longitude,
            @Schema(description = "纬度") BigDecimal latitude
    ) {}

    // ── 搜索内部小区 ──

    @Schema(description = "内部小区搜索响应")
    public record SearchResponse(
            @Schema(description = "匹配的小区列表") List<CommunityView> items
    ) {}

    // ── 代理高德 POI 搜索 ──

    @Schema(description = "代理高德 POI 搜索响应")
    public record PoiSearchResponse(
            @Schema(description = "POI 列表") List<PoiView> items
    ) {}

    // ── 确认导入小区 ──

    @Schema(description = "从地图 POI 导入小区的请求")
    public record ImportRequest(
            @NotBlank @Schema(description = "地图供应商", example = "amap") String mapProvider,
            @NotBlank @Schema(description = "地图平台 POI ID", example = "B00170A123") String externalPoiId
    ) {}

    @Schema(description = "从地图 POI 导入小区的响应")
    public record ImportResponse(
            @Schema(description = "内部小区 ID") String id,
            @Schema(description = "名称") String name,
            @Schema(description = "省") String province,
            @Schema(description = "市") String city,
            @Schema(description = "区") String district,
            @Schema(description = "地址") String address,
            @Schema(description = "经度") BigDecimal longitude,
            @Schema(description = "纬度") BigDecimal latitude
    ) {}
}
