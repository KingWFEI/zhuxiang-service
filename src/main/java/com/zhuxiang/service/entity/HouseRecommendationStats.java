package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("house_recommendation_stats")
public class HouseRecommendationStats {
    @TableId
    private String houseId;
    private Long exposureCount;
    private Long detailClickCount;
    private Long effectiveViewCount;
    private Long favoriteCount;
    private Long appointmentCount;
    private Long orderCount;
    private Long notInterestedCount;
    private LocalDateTime updatedAt;
}
