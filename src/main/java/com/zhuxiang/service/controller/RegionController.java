package com.zhuxiang.service.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.RegionDtos;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Validated
@RestController
@RequestMapping("/regions")
@Tag(name = "行政区域", description = "按当前城市提供找房区域选项")
public class RegionController {
    private static final Logger log = LoggerFactory.getLogger(RegionController.class);
    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping("/districts")
    @Operation(summary = "查询城市下属行政区")
    public ApiResponse<List<RegionDtos.DistrictOption>> districts(
            @Parameter(description = "定位或手动选择的地级市名称", example = "重庆市")
            @RequestParam @NotBlank @Size(max = 50) String city
    ) {
        String normalizedCity = city.trim();
        String cityWithoutSuffix = normalizedCity.endsWith("市")
                ? normalizedCity.substring(0, normalizedCity.length() - 1)
                : normalizedCity;
        Region configuredCity = regionService.getOne(
                Wrappers.<Region>lambdaQuery()
                        .eq(Region::getLevel, "city")
                        .and(query -> query.eq(Region::getName, normalizedCity)
                                .or().eq(Region::getName, cityWithoutSuffix)
                                .or().eq(Region::getName, cityWithoutSuffix + "市")
                                .or().eq(Region::getCode, normalizedCity))
                        .last("LIMIT 1"), false);
        List<Region> localDistricts = configuredCity == null
                ? List.of()
                : regionService.list(Wrappers.<Region>lambdaQuery()
                        .eq(Region::getParentId, configuredCity.getId())
                        .eq(Region::getLevel, "district")
                        .eq(Region::getEnabled, 1)
                        .orderByAsc(Region::getSortOrder));
        if (configuredCity == null) {
            log.info("本地区域未配置城市: city={}", normalizedCity);
        }
        List<RegionDtos.DistrictOption> fallback = localDistricts
                .stream()
                .map(region -> new RegionDtos.DistrictOption(region.getName(), region.getCode()))
                .toList();
        return ApiResponse.success(fallback);
    }
}
