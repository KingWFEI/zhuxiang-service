package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName(value = "lease_termination_applications")
@Data
public class LeaseTerminationApplication implements Serializable {

    public static final String STATUS_PENDING_REVIEW = "pending_review";
    public static final String STATUS_NEED_SUPPLEMENT = "need_supplement";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_INSPECTION_PENDING = "inspection_pending";
    public static final String STATUS_SETTLEMENT_PENDING = "settlement_pending";
    public static final String STATUS_REFUND_PENDING = "refund_pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";

    public static final String ACTION_APPLIED = "applied";
    public static final String ACTION_APPROVED = "approved";
    public static final String ACTION_REJECTED = "rejected";
    public static final String ACTION_SUPPLEMENT_REQUESTED = "supplement_requested";
    public static final String ACTION_SUPPLEMENTED = "supplemented";
    public static final String ACTION_CANCELLED = "cancelled";
    public static final String ACTION_INSPECTION_COMPLETED = "inspection_completed";
    public static final String ACTION_SETTLEMENT_CONFIRMED = "settlement_confirmed";
    public static final String ACTION_REFUND_COMPLETED = "refund_completed";

    @TableId
    private String id;

    private String applicationNo;

    private String tenantId;

    private String contractId;

    private String houseId;

    private String roomId;

    private String reason;

    private LocalDate expectedMoveOutDate;

    private Boolean hasMovedOut;

    private String contactName;

    private String contactPhone;

    private String remark;

    private String attachments;

    private String status;

    private String rejectReason;

    private String supplementReason;

    private String auditUserId;

    private LocalDateTime auditTime;

    private String cancelReason;

    private LocalDateTime cancelTime;

    private String inspectionResult;

    private LocalDateTime inspectionCompletedTime;

    private String settlementDetail;

    private Integer totalDeduction;

    private Integer refundAmount;

    private LocalDateTime settlementConfirmedTime;

    private LocalDateTime refundCompletedTime;

    private LocalDate actualMoveOutDate;

    private LocalDateTime completedTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
