package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("appointment_access_grant")
@ToString(exclude = "keyboardPwdCiphertext")
public class AppointmentAccessGrant {
    @TableId
    private String id;
    private String appointmentId;
    private String tenantId;
    private String houseId;
    private String smartLockId;
    private Long ttlockLockId;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String status;
    private String ekeyStatus;
    private Long ttlockKeyId;
    private String receiverUsername;
    private String ekeyErrorMessage;
    private String passcodeStatus;
    private Long ttlockKeyboardPwdId;
    private String keyboardPwdCiphertext;
    private Integer keyboardPwdType;
    private String passcodeErrorMessage;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime grantedAt;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
