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
            String rentMode,
            String rentType,
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
            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "房源发布来源：LANDLORD房东发布，PLATFORM平台自营",
                    allowableValues = {"LANDLORD", "PLATFORM"}
            )
            String sourceType,
            String status,
            boolean isRented,
            String rentAvailability,
            String activeOrderId,
            boolean activeOrderBelongsToMe
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
            String rentMode,
            String rentType,
            String roomType,
            Integer area,
            String floor,
            String orientation,
            List<String> tags,
            List<String> facilities,
            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "带管理端图标配置的设施列表"
            )
            List<FacilityView> facilityItems,
            String description,
            boolean isSmartLockSupported,
            boolean isFavorite,
            String metro,
            String decoration,
            LocalDate availableDate,
            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "房源发布来源：LANDLORD房东发布，PLATFORM平台自营",
                    allowableValues = {"LANDLORD", "PLATFORM"}
            )
            String sourceType,
            String landlordId,
            String landlordName,
            String avatarUrl,
            boolean isVerified,
            BigDecimal rating,
            Integer rentedCount,
            String responseDescription,
            String status,
            boolean isRented,
            String rentAvailability,
            String activeOrderId,
            boolean activeOrderBelongsToMe,
            BigDecimal longitude,
            BigDecimal latitude,
            @io.swagger.v3.oas.annotations.media.Schema(description = "房东公开资料")
            LandlordDtos.ProfileView landlordProfile
    ) {
    }

    public record FacilityView(
            @io.swagger.v3.oas.annotations.media.Schema(description = "设施ID")
            String id,
            @io.swagger.v3.oas.annotations.media.Schema(description = "设施名称")
            String name,
            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "管理端配置的跨端图标键",
                    example = "wifi"
            )
            String iconKey
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

    public record HotCommunityItem(
            String name,
            String district,
            int startingRent,
            int colorValue
    ) {}
}
