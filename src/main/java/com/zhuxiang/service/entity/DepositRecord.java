package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "deposit_record")
@Data
public class DepositRecord implements Serializable {

    @TableId
    private String id;

    private String leaseId;

    private String userId;

    private String houseId;

    private Integer amount;

    private Integer withheldAmount;

    private Integer refundedAmount;

    private String status;

    private String paymentRecordId;

    private String refundPaymentRecordId;

    private String refundChannel;

    private String refundTradeNo;

    private String settlementDetail;

    private LocalDateTime terminatedAt;

    private LocalDateTime refundedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
