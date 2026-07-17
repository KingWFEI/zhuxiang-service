package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 押金扣款明细。按验收项逐条记录，可追溯入住/退租照片。
 */
@TableName("deposit_deduction_item")
@Data
public class DepositDeductionItem implements Serializable {

    public static final String RESULT_UNCHANGED = "UNCHANGED";
    public static final String RESULT_NORMAL_WEAR = "NORMAL_WEAR";
    public static final String RESULT_NEW_DAMAGE = "NEW_DAMAGE";
    public static final String RESULT_MISSING = "MISSING";
    public static final String RESULT_DISPUTED = "DISPUTED";

    public static final String TENANT_ACCEPT = "ACCEPT";
    public static final String TENANT_DISPUTE = "DISPUTE";

    @TableId
    private String id;

    private String contractId;

    private String snapshotId;

    private String roomCode;

    private String itemCode;

    /** UNCHANGED / NORMAL_WEAR / NEW_DAMAGE / MISSING / DISPUTED */
    private String result;

    private String reason;

    /** 扣款金额（分） */
    private Integer deductionAmount;

    /** JSON 数组：证据照片 URL 列表 */
    private String evidenceUrls;

    /** ACCEPT / DISPUTE */
    private String tenantStatus;

    private String tenantDisputeReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
