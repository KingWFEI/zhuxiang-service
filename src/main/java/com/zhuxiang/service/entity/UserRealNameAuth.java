package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实名认证记录
 */
@TableName(value = "user_real_name_auth")
@Data
public class UserRealNameAuth implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String realNameAuthNo;

    private String esignFaceFlowId;

    private String esignPsnId;

    private String authStatus;

    private String realName;

    private String accountMobile;

    private String verifiedMobile;

    private String idCardType;

    @ToString.Exclude
    private String idCardCiphertext;

    private String idCardMasked;

    @ToString.Exclude
    private String authUrl;

    private LocalDateTime authUrlExpireTime;

    private String failureCode;

    private String failureMessage;

    private String expireReason;

    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer version;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
