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

    private String paymentNo;

    private String orderId;

    private String billId;

    private String leaseId;

    private String houseId;

    private String houseName;

    private String userId;

    private Integer amount;

    private String paymentChannel;

    private String channelTradeNo;

    private String status;

    private String type;

    private String feeBreakdown;

    private String remark;

    private LocalDateTime paidAt;

    private LocalDateTime callbackTime;

    private String callbackPayload;

    private String refundToRecordId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
