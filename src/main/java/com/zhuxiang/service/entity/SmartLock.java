package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能门锁绑定表。
 */
@TableName(value = "smart_locks")
@Data
@ToString(exclude = "lockData")
public class SmartLock implements Serializable {

    /**
     * 主键ID。
     */
    @TableId
    private String id;

    /**
     * 绑定的房源ID。
     */
    private String houseId;

    /**
     * 绑定的房间ID，可为空。
     */
    private String roomId;

    /**
     * 通通锁开放平台返回的门锁ID。
     */
    private Long lockId;

    /**
     * 通通锁开放平台返回的管理员eKey ID。
     */
    private Long keyId;

    /**
     * TTLock 键盘密码版本，生成期限密码前从平台详情同步。
     */
    private Integer keyboardPwdVersion;

    /**
     * 门锁时区相对 UTC 的毫秒偏移量。
     */
    private Long timezoneRawOffset;

    /**
     * 门锁名称。
     */
    private String lockName;

    /**
     * 门锁MAC地址。
     */
    private String lockMac;

    /**
     * 通通锁SDK生成的门锁控制数据。
     */
    private String lockData;

    /**
     * 门锁绑定状态。
     */
    private String status;

    /**
     * 开放平台同步失败错误码。
     */
    private String platformErrorCode;

    /**
     * 开放平台同步失败错误信息。
     */
    private String platformErrorMessage;

    /**
     * 最近一次同步开放平台时间。
     */
    private LocalDateTime lastSyncTime;

    /**
     * 门锁电量。
     */
    private Integer battery;

    /**
     * 初始化扫描时的蓝牙信号强度。
     */
    private Integer rssi;

    /**
     * 最近一次蓝牙状态刷新时间。
     */
    private LocalDateTime lastBleSyncTime;

    /**
     * 电量来源。
     */
    private String batterySource;

    /**
     * 绑定时间。
     */
    private LocalDateTime bindTime;

    /**
     * 初始化绑定操作人ID。
     */
    private String createdBy;

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
