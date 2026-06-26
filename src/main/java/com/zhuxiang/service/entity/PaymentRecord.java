package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "payment_record")
@Data
public class PaymentRecord implements Serializable {

    @TableId
    private String id;

    private String orderId;

    private String userId;

    private Integer amount;

    private String paymentChannel;

    private String channelTradeNo;

    private String status;

    private String feeBreakdown;

    private LocalDateTime paidAt;

    private LocalDateTime callbackTime;

    private String callbackPayload;

    private String refundToRecordId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
