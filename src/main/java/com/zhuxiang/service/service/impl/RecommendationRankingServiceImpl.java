package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.RecommendationEventType;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HouseRecommendationStats;
import com.zhuxiang.service.entity.RecommendationEvent;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.HouseRecommendationStatsMapper;
import com.zhuxiang.service.service.RecommendationEventService;
import com.zhuxiang.service.service.RecommendationRankingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationRankingServiceImpl implements RecommendationRankingService {
    private static final int MAX_PROFILE_EVENTS = 300;
    private static final Map<RecommendationEventType, Double> EVENT_WEIGHTS = Map.of(
            RecommendationEventType.DETAIL_CLICK, 1.0,
            RecommendationEventType.EFFECTIVE_VIEW, 2.0,
            RecommendationEventType.FAVORITE, 5.0,
            RecommendationEventType.UNFAVORITE, -4.0,
            RecommendationEventType.APPOINTMENT, 10.0,
            RecommendationEventType.ORDER_CREATED, 15.0,
            RecommendationEventType.NOT_INTERESTED, -6.0
    );

    private final RecommendationEventService eventService;
    private final HouseRecommendationStatsMapper statsMapper;
    private final HouseMapper houseMapper;

    public RecommendationRankingServiceImpl(
            RecommendationEventService eventService,
            HouseRecommendationStatsMapper statsMapper,
            HouseMapper houseMapper
    ) {
        this.eventService = eventService;
        this.statsMapper = statsMapper;
        this.houseMapper = houseMapper;
    }

    @Override
    public List<House> rank(List<House> candidates, String userId) {
        if (candidates.isEmpty()) return List.of();
        UserPreference preference = buildPreference(userId);
        Map<String, HouseRecommendationStats> statsByHouse = loadStats(candidates);
        List<ScoredHouse> scored = candidates.stream()
                .map(house -> new ScoredHouse(
                        house,
                        score(house, preference, statsByHouse.get(house.getId()), userId)
                ))
                .sorted(Comparator.comparingDouble(ScoredHouse::score).reversed()
                        .thenComparing(value -> Objects.requireNonNullElse(
                                value.house().getCreatedAt(), LocalDateTime.MIN),
                                Comparator.reverseOrder())
                        .thenComparing(value -> value.house().getId()))
                .toList();
        return diversify(scored);
    }

    private UserPreference buildPreference(String userId) {
        List<RecommendationEvent> events = eventService.recentUserEvents(userId, MAX_PROFILE_EVENTS);
        Set<String> eventHouseIds = events.stream()
                .map(RecommendationEvent::getHouseId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Map<String, House> eventHouses = eventHouseIds.isEmpty() ? Map.of()
                : houseMapper.selectBatchIds(eventHouseIds).stream()
                        .collect(Collectors.toMap(House::getId, Function.identity()));

        UserPreference preference = new UserPreference();
        LocalDateTime now = LocalDateTime.now();
        for (RecommendationEvent event : events) {
            RecommendationEventType type;
            try {
                type = RecommendationEventType.valueOf(event.getEventType());
            } catch (RuntimeException ignored) {
                continue;
            }
            double baseWeight = EVENT_WEIGHTS.getOrDefault(type, 0.0);
            long ageDays = Math.max(0, Duration.between(event.getCreatedAt(), now).toDays());
            double halfLife = type == RecommendationEventType.DETAIL_CLICK
                    || type == RecommendationEventType.EFFECTIVE_VIEW ? 7.0 : 30.0;
            double weight = baseWeight * Math.pow(0.5, ageDays / halfLife);
            if (type == RecommendationEventType.EXPOSURE) {
                preference.exposureCounts.merge(event.getHouseId(), 1, Integer::sum);
                continue;
            }
            if (weight < 0) {
                preference.negativeHouseWeights.merge(event.getHouseId(), -weight, Double::sum);
                continue;
            }
            House house = eventHouses.get(event.getHouseId());
            if (house == null || weight <= 0) continue;
            preference.totalWeight += weight;
            preference.priceWeightedSum += Objects.requireNonNullElse(house.getPrice(), 0) * weight;
            addWeight(preference.locations, house.getLocation(), weight);
            addWeight(preference.communities, house.getCommunityId(), weight);
            addWeight(preference.roomTypes, house.getRoomType(), weight);
            addWeight(preference.rentModes, house.getRentMode(), weight);
            addWeight(preference.rentTypes, house.getRentType(), weight);
            preference.positiveHouseIds.add(house.getId());
        }
        return preference;
    }

    private double score(House house, UserPreference preference,
                         HouseRecommendationStats stats, String userId) {
        double popularity = Math.min(1.0,
                Math.log1p(Objects.requireNonNullElse(house.getFavoriteCount(), 0)) / Math.log(101));
        long ageDays = house.getCreatedAt() == null ? 90
                : Math.max(0, Duration.between(house.getCreatedAt(), LocalDateTime.now()).toDays());
        double freshness = Math.exp(-ageDays / 30.0);
        double quality = qualityScore(stats);
        double result = 0.08 * quality + 0.07 * freshness + 0.05 * popularity
                + 0.05 * stableExploration(userId, house.getId());

        if (preference.totalWeight <= 0) {
            result += 0.55 * quality + 0.20 * popularity;
        } else {
            double preferredPrice = preference.priceWeightedSum / preference.totalWeight;
            double tolerance = Math.max(preferredPrice * 0.35, 1.0);
            double priceMatch = Math.exp(-Math.abs(
                    Objects.requireNonNullElse(house.getPrice(), 0) - preferredPrice) / tolerance);
            result += 0.25 * match(preference.locations, house.getLocation(), preference.totalWeight)
                    + 0.20 * priceMatch
                    + 0.15 * match(preference.roomTypes, house.getRoomType(), preference.totalWeight)
                    + 0.05 * match(preference.communities, house.getCommunityId(), preference.totalWeight)
                    + 0.05 * match(preference.rentModes, house.getRentMode(), preference.totalWeight)
                    + 0.05 * match(preference.rentTypes, house.getRentType(), preference.totalWeight);
        }

        int repeatExposures = preference.exposureCounts.getOrDefault(house.getId(), 0);
        if (!preference.positiveHouseIds.contains(house.getId())) {
            result -= Math.min(0.20, repeatExposures * 0.04);
        }
        result -= Math.min(0.60,
                preference.negativeHouseWeights.getOrDefault(house.getId(), 0.0) * 0.08);
        return result;
    }

    private double qualityScore(HouseRecommendationStats stats) {
        if (stats == null) return 0.35;
        double exposures = Objects.requireNonNullElse(stats.getExposureCount(), 0L);
        double clicks = Objects.requireNonNullElse(stats.getDetailClickCount(), 0L);
        double favorites = Objects.requireNonNullElse(stats.getFavoriteCount(), 0L);
        double appointments = Objects.requireNonNullElse(stats.getAppointmentCount(), 0L);
        double clickRate = (clicks + 5 * 0.08) / (exposures + 5);
        double favoriteRate = (favorites + 10 * 0.02) / (exposures + 10);
        double appointmentRate = (appointments + 20 * 0.01) / (exposures + 20);
        return Math.min(1.0, 0.30 * clickRate / 0.20
                + 0.30 * favoriteRate / 0.08
                + 0.40 * appointmentRate / 0.04);
    }

    private List<House> diversify(List<ScoredHouse> scored) {
        List<House> result = new ArrayList<>(scored.size());
        List<House> deferred = new ArrayList<>();
        Map<String, Integer> communityCounts = new HashMap<>();
        Map<String, Integer> landlordCounts = new HashMap<>();
        for (ScoredHouse value : scored) {
            House house = value.house();
            if (count(communityCounts, house.getCommunityId()) >= 2
                    || count(landlordCounts, house.getLandlordId()) >= 2) {
                deferred.add(house);
                continue;
            }
            result.add(house);
            increment(communityCounts, house.getCommunityId());
            increment(landlordCounts, house.getLandlordId());
        }
        result.addAll(deferred);
        return result;
    }

    private Map<String, HouseRecommendationStats> loadStats(List<House> candidates) {
        List<String> ids = candidates.stream().map(House::getId).toList();
        return statsMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(
                HouseRecommendationStats::getHouseId, Function.identity()));
    }

    private double stableExploration(String userId, String houseId) {
        String key = Objects.requireNonNullElse(userId, "guest") + ':'
                + LocalDate.now(ZoneOffset.UTC) + ':' + houseId;
        return Math.floorMod(key.hashCode(), 1000) / 999.0;
    }

    private void addWeight(Map<String, Double> values, String key, double weight) {
        if (StringUtils.hasText(key)) values.merge(key, weight, Double::sum);
    }

    private double match(Map<String, Double> values, String key, double totalWeight) {
        if (!StringUtils.hasText(key) || totalWeight <= 0) return 0;
        return Math.min(1.0, values.getOrDefault(key, 0.0) / totalWeight);
    }

    private int count(Map<String, Integer> counts, String key) {
        return StringUtils.hasText(key) ? counts.getOrDefault(key, 0) : 0;
    }

    private void increment(Map<String, Integer> counts, String key) {
        if (StringUtils.hasText(key)) counts.merge(key, 1, Integer::sum);
    }

    private record ScoredHouse(House house, double score) {}

    private static final class UserPreference {
        private double totalWeight;
        private double priceWeightedSum;
        private final Map<String, Double> locations = new LinkedHashMap<>();
        private final Map<String, Double> communities = new LinkedHashMap<>();
        private final Map<String, Double> roomTypes = new LinkedHashMap<>();
        private final Map<String, Double> rentModes = new LinkedHashMap<>();
        private final Map<String, Double> rentTypes = new LinkedHashMap<>();
        private final Map<String, Integer> exposureCounts = new HashMap<>();
        private final Map<String, Double> negativeHouseWeights = new HashMap<>();
        private final Set<String> positiveHouseIds = new HashSet<>();
    }
}
