package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租约验收快照。签约时从房源模板复制，锁定后不可修改。
 */
@TableName("lease_inspection_snapshot")
@Data
public class LeaseInspectionSnapshot implements Serializable {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_TENANT_CONFIRMED = "TENANT_CONFIRMED";
    public static final String STATUS_LOCKED = "LOCKED";

    @TableId
    private String id;

    private String contractId;

    private String leaseId;

    private String houseId;

    private Integer templateVersion;

    /** JSON 数组：同模板 rooms 结构，不可变快照 */
    private String rooms;

    private String status;

    /** 审批人用户 ID */
    private String reviewedBy;

    /** 审批时间 */
    private LocalDateTime reviewedAt;

    /** APPROVE / REJECT */
    private String reviewAction;

    /** 审批意见或驳回原因 */
    private String reviewComment;

    private LocalDateTime moveInSubmittedAt;

    private String moveInSubmittedBy;

    private LocalDateTime moveOutSubmittedAt;

    private String moveOutSubmittedBy;

    private LocalDateTime completedAt;

    /** 验房完成人用户 ID（管理端锁定验房时记录） */
    private String completedBy;

    /** 验房完成备注 */
    private String completionComment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
