package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment_status_log")
public class AppointmentStatusLog {
    @TableId
    private String id;
    private String appointmentId;
    private String fromStatus;
    private String toStatus;
    private String operatorId;
    private String operatorRole;
    private String reason;
    private String metadataJson;
    private LocalDateTime createdAt;
}
