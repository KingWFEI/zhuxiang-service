package com.zhuxiang.service.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.entity.Region;
import com.zhuxiang.service.service.RegionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Initializes administrative regions from a JSON resource when the region table is empty. */
@Component
public class RegionInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegionInitializer.class);
    private static final Set<String> LEVELS = Set.of("province", "city", "district", "business_area");

    private final RegionService regionService;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String initialDataLocation;

    public RegionInitializer(
            RegionService regionService,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${app.region.initial-data:classpath:region-data.json}") String initialDataLocation
    ) {
        this.regionService = regionService;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.initialDataLocation = initialDataLocation;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long existingCount = regionService.count(Wrappers.<Region>lambdaQuery());
        if (existingCount > 0) {
            log.info("Found {} administrative regions; skipping JSON initialization.", existingCount);
            return;
        }

        List<RegionSeed> seedData = loadSeedData();
        List<RegionSeed> orderedSeeds = new ArrayList<>(seedData);
        if (orderedSeeds.stream().anyMatch(seed -> seed == null)) {
            throw new IllegalStateException("Administrative region JSON contains a null item");
        }
        orderedSeeds.sort(Comparator
                .comparingInt((RegionSeed seed) -> levelOrder(seed.level()))
                .thenComparingInt(seed -> seed.sortOrder() == null ? 0 : seed.sortOrder()));

        LocalDateTime now = LocalDateTime.now();
        List<Region> regions = new ArrayList<>(orderedSeeds.size());
        Map<String, String> idsByCode = new HashMap<>();
        Map<String, String> levelsByCode = new HashMap<>();
        Set<String> seenCodes = new HashSet<>();

        for (RegionSeed seed : orderedSeeds) {
            String name = trimToNull(seed.name());
            String code = trimToNull(seed.code());
            String level = trimToNull(seed.level());
            String parentCode = trimToNull(seed.parentCode());

            if (name == null || level == null || !LEVELS.contains(level)) {
                throw new IllegalStateException("Administrative region JSON contains an invalid item: " + seed);
            }
            if (code != null && !seenCodes.add(code)) {
                throw new IllegalStateException("Duplicate administrative region code in JSON: " + code);
            }

            String parentId = null;
            if ("province".equals(level)) {
                if (parentCode != null) {
                    throw new IllegalStateException("Province region must not define parentCode: " + name);
                }
            } else if ("city".equals(level) && parentCode == null) {
                // 直辖市以城市层级作为根节点，例如数据中的重庆市。
            } else {
                if (parentCode == null || !idsByCode.containsKey(parentCode)) {
                    throw new IllegalStateException("Cannot find parentCode " + parentCode + " for region " + name);
                }
                String expectedParentLevel = switch (level) {
                    case "city" -> "province";
                    case "district" -> "city";
                    case "business_area" -> "district";
                    default -> throw new IllegalStateException(
                            "Unsupported administrative region level in JSON: " + level);
                };
                if (!expectedParentLevel.equals(levelsByCode.get(parentCode))) {
                    throw new IllegalStateException("Invalid parent level for administrative region: " + name);
                }
                parentId = idsByCode.get(parentCode);
            }

            Region region = new Region();
            region.setId(UUID.randomUUID().toString());
            region.setParentId(parentId);
            region.setName(name);
            region.setCode(code);
            region.setLevel(level);
            region.setSortOrder(seed.sortOrder() == null ? 0 : seed.sortOrder());
            region.setEnabled(Boolean.FALSE.equals(seed.enabled()) ? 0 : 1);
            region.setCreatedAt(now);
            region.setUpdatedAt(now);
            regions.add(region);

            if (code != null) {
                idsByCode.put(code, region.getId());
                levelsByCode.put(code, level);
            }
        }

        if (regions.isEmpty()) {
            throw new IllegalStateException("Administrative region JSON is empty: " + initialDataLocation);
        }
        if (!regionService.saveBatch(regions)) {
            throw new IllegalStateException("Failed to initialize administrative regions from JSON");
        }
        log.info("Initialized {} administrative regions from {}.", regions.size(), initialDataLocation);
    }

    private List<RegionSeed> loadSeedData() {
        Resource resource = resourceLoader.getResource(initialDataLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("Administrative region JSON file not found: " + initialDataLocation);
        }
        try (InputStream input = resource.getInputStream()) {
            List<RegionSeed> seedData = objectMapper.readValue(input, new TypeReference<List<RegionSeed>>() {});
            if (seedData == null) {
                throw new IllegalStateException("Administrative region JSON must contain an array");
            }
            return seedData;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read administrative region JSON: " + initialDataLocation, exception);
        }
    }

    private static int levelOrder(String level) {
        return switch (level) {
            case "province" -> 0;
            case "city" -> 1;
            case "district" -> 2;
            case "business_area" -> 3;
            default -> throw new IllegalStateException(
                    "Unsupported administrative region level in JSON: " + level);
        };
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim();
    }

    public record RegionSeed(
            String name,
            String code,
            String level,
            String parentCode,
            Integer sortOrder,
            Boolean enabled
    ) {}
}
