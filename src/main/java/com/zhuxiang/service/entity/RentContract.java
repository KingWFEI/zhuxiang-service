package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName(value = "rent_contract")
@Data
public class RentContract implements Serializable {

    @TableId
    private String id;

    private String orderId;

    private String userId;

    private String houseId;

    private String contractNo;

    private String status;

    private String tenantName;

    private String tenantPhone;

    private String tenantIdCard;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer leaseMonths;

    private Integer monthlyRent;

    private Integer deposit;

    private Integer serviceFee;

    private String houseName;

    private String roomName;

    private String houseAddress;

    private LocalDateTime signedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
