package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 租约对应的 TTLock 期限密码权限。
 */
@Data
@TableName("lock_passcode_permission")
@ToString(exclude = "keyboardPwdCiphertext")
public class LockPasscodePermission implements Serializable {

    @TableId
    private String id;
    private String leaseId;
    private String tenantId;
    private String smartLockId;
    private Long ttlockLockId;
    private Long ttlockKeyboardPwdId;
    private String keyboardPwdCiphertext;
    private Integer keyboardPwdType;
    private Integer keyboardPwdVersion;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private String deviceSyncStatus;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
