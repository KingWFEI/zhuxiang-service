package com.zhuxiang.service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class LeaseContractFillData {
    // 甲方（房东）—— 来自 Landlord 表 + Landlord.idCardCiphertext 解密
    private String lessorName;
    private String lessorMobile;
    private String lessorIdCard;

    // 乙方（租户）—— 来自 UserRealNameAuth
    private String tenantName;
    private String tenantMobile;
    private String tenantIdCard;

    // 房源地址 —— 来自 House
    private String houseAddress;

    // 租期（年）—— 来自 RentOrder.leaseMonths / 12
    private Integer leaseMonths;

    // 起租/截止日期 —— 来自 RentOrder
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;

    // 不续租提前告知（月）—— 默认 1
    private Integer noticeMonths;

    // 押金 / 月租金 —— 来自 RentOrder
    private BigDecimal deposit;
    private BigDecimal monthlyRent;

    // 租金交付日 —— 用起租日期
    private LocalDate rentPaymentDate;

    // 签字日期 —— 填写合同生成日期
    private LocalDate lessorSignDate;
    private LocalDate tenantSignDate;
}
