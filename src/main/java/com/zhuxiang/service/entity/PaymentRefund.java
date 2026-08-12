package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_refund")
public class PaymentRefund {
    @TableId
    private String id;
    private String refundNo;
    private String paymentRecordId;
    private String orderId;
    private String userId;
    private Integer refundAmount;
    private String refundReason;
    private String triggerType;
    private String status;
    private String channelTradeNo;
    private Integer attemptCount;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
