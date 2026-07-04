package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 开锁结果审计记录（手动蓝牙 + 无感），不包含任何门锁控制凭证。 */
@TableName("unlock_record")
@Data
public class UnlockRecord implements Serializable {

    @TableId
    private String id;

    private String userId;

    private String leaseId;

    private String smartLockId;

    private Long ttlockLockId;

    private String triggerType;

    private Integer rssi;

    private Integer stableMillis;

    private String result;

    private String failureReason;

    private String deviceInfo;

    private String appVersion;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
