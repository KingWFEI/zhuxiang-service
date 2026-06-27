package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName(value = "rent_order")
@Data
public class RentOrder implements Serializable {

    @TableId
    private String id;

    private String userId;

    private String houseId;

    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer leaseMonths;

    private String paymentMethod;

    private Integer paymentMonths;

    private Integer tenantCount;

    private String tenantName;

    private String tenantPhone;

    private String tenantIdCard;

    private Integer monthlyRent;

    private Integer deposit;

    private Integer serviceFee;

    private Integer firstPaymentAmount;

    private Integer totalAmount;

    private LocalDateTime realNameAt;

    private LocalDateTime contractConfirmedAt;

    private LocalDateTime paidAt;

    private LocalDateTime signedAt;

    private LocalDateTime cancelledAt;

    private Integer userHidden;

    private LocalDateTime hiddenAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
