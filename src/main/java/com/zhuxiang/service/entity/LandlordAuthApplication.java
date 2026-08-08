package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("landlord_auth_application")
public class LandlordAuthApplication implements Serializable {
    @TableId
    private String id;
    private String applicationNo;
    private String userId;
    private String status;
    private String realName;
    @ToString.Exclude
    private String idCardCiphertext;
    private String idCardMasked;
    @ToString.Exclude
    private String idCardFrontUrl;
    @ToString.Exclude
    private String idCardBackUrl;
    private String contactPhone;
    private String contactWechat;
    private String contactEmail;
    private String contactAddress;
    private String preferredContactTime;
    private String applicantNote;
    private String rejectReason;
    private String reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
