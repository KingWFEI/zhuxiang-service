package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 房源房产证明材料。每次替换新增一条记录，历史记录不覆盖。
 */
@Data
@TableName("house_property_certificate")
public class HousePropertyCertificate implements Serializable {

    @TableId
    private String id;

    private String houseId;

    private String landlordId;

    private String originalName;

    private String objectKey;

    private String contentType;

    private Long fileSize;

    /** pending、approved、rejected。 */
    private String auditStatus;

    private Integer isCurrent;

    private String reviewRemark;

    private String reviewerId;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;

    private static final long serialVersionUID = 1L;
}

