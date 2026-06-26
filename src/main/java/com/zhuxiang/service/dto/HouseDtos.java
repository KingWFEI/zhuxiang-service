package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class HouseDtos {

    private HouseDtos() {
    }

    public record HouseView(
            String id,
            String title,
            String coverImage,
            String location,
            String community,
            Integer price,
            String roomType,
            Integer area,
            String floor,
            String orientation,
            List<String> tags,
            List<String> facilities,
            String description,
            boolean isSmartLockSupported,
            boolean isFavorite,
            String metro,
            String decoration,
            LocalDate availableDate,
            String status
    ) {
    }

    public record HouseDetail(
            String id,
            String title,
            String coverImage,
            List<String> images,
            String location,
            String community,
            String address,
            Integer price,
            Integer deposit,
            String paymentMethod,
            String roomType,
            Integer area,
            String floor,
            String orientation,
            List<String> tags,
            List<String> facilities,
            String description,
            boolean isSmartLockSupported,
            boolean isFavorite,
            String metro,
            String decoration,
            LocalDate availableDate,
            String landlordId,
            String landlordName,
            String avatarUrl,
            boolean isVerified,
            BigDecimal rating,
            Integer rentedCount,
            String responseDescription,
            String status
    ) {
    }

    public record AdvertisementView(
            String id,
            String title,
            String description,
            String imageUrl,
            String targetType,
            String targetValue
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FeedItem(String type, HouseView house, AdvertisementView advertisement) {

        public static FeedItem house(HouseView house) {
            return new FeedItem("house", house, null);
        }

        public static FeedItem advertisement(AdvertisementView advertisement) {
            return new FeedItem("advertisement", null, advertisement);
        }
    }

    public record FeedData(
            List<FeedItem> items,
            long page,
            long pageSize,
            boolean hasMore
    ) {
    }

    public record FavoriteResult(String houseId, boolean isFavorite) {
    }

    public record Option(String label, String value) {
    }

    public record PriceRange(String label, int minPrice, int maxPrice) {
    }

    public record FilterOptions(
            List<Option> regions,
            List<PriceRange> priceRanges,
            List<Option> roomTypes,
            List<Option> facilities,
            List<Option> sortOptions
    ) {
    }

    public record LandlordView(
            String id,
            String name,
            String avatarUrl,
            boolean isVerified,
            BigDecimal rating,
            Integer rentedCount,
            String responseDescription
    ) {
    }
}
