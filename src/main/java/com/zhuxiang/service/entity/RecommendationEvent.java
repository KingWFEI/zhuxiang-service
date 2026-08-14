package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recommendation_event")
public class RecommendationEvent {
    @TableId
    private String id;
    private String userId;
    private String anonymousId;
    private String sessionId;
    private String requestId;
    private String houseId;
    private String eventType;
    private Integer positionIndex;
    private Long durationMs;
    private String sourcePage;
    private LocalDateTime createdAt;
}
