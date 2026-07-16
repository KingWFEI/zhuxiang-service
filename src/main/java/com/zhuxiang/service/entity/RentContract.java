package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

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

    private String docTemplateId;

    private String contractFileId;

    private String signFlowId;

    private String contractNum;

    private Integer lessorSigned;

    private Integer tenantSigned;

    private String previewUrl;

    private String failureCode;

    private String failureMessage;

    private Integer version;

    private String tenantName;

    private String tenantPhone;

    private String tenantIdCard;

    @ToString.Exclude
    private String tenantIdCardCiphertext;

    private String landlordName;

    private String landlordPhone;

    @ToString.Exclude
    private String landlordIdCard;

    @ToString.Exclude
    private String landlordIdCardCiphertext;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer leaseMonths;

    private Integer monthlyRent;

    private Integer deposit;

    private Integer serviceFee;

    private Integer paymentMonths;

    private Integer firstPaymentAmount;

    private String idCardFrontUrl;

    private String idCardBackUrl;

    private String houseName;

    private String roomName;

    private String houseAddress;

    private LocalDateTime signedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
