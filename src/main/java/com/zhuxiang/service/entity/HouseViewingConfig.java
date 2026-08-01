package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("house_viewing_config")
public class HouseViewingConfig {
    @TableId
    private String houseId;
    private Integer enabled;
    private String viewingMode;
    private Integer durationMinutes;
    private Integer advanceMinMinutes;
    private Integer advanceMaxDays;
    private Integer confirmationTimeoutMinutes;
    private Integer rescheduleTimeoutMinutes;
    private Integer lockGrantLeadMinutes;
    private Integer lockValidBeforeMinutes;
    private Integer lockValidAfterMinutes;
    private String timezone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
