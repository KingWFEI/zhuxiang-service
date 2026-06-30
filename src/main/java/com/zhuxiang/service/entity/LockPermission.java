package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租客智能门锁权限记录。
 */
@TableName(value = "lock_permission")
@Data
public class LockPermission implements Serializable {

    /**
     * 权限记录ID。
     */
    @TableId
    private String id;

    /**
     * 关联租约ID。
     */
    private String leaseId;

    /**
     * 租客用户ID。
     */
    private String tenantId;

    /**
     * 房源ID，当前作为房间唯一标识。
     */
    private String houseId;

    /**
     * smart_locks表主键ID。
     */
    private String smartLockId;

    /**
     * TTLock平台门锁ID。
     */
    private Long ttlockLockId;

    /**
     * TTLock平台下发的租客eKey ID。
     */
    private Long ttlockKeyId;

    /**
     * TTLock接收账号，使用租客手机号或邮箱。
     */
    private String receiverUsername;

    /**
     * 权限类型，当前固定为EKEY。
     */
    private String permissionType;

    /**
     * 权限状态：ACTIVE、FAILED、REVOKED、REVOKE_FAILED、EXPIRED。
     */
    private String status;

    /**
     * 最近一次下发失败原因。
     */
    private String errorMessage;

    /**
     * eKey有效期开始时间。
     */
    private LocalDateTime startTime;

    /**
     * eKey有效期结束时间。
     */
    private LocalDateTime endTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
