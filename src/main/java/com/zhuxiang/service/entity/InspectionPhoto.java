package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 验收照片。关联合同、房间、设施项和验收阶段。
 */
@TableName("inspection_photo")
@Data
public class InspectionPhoto implements Serializable {

    public static final String STAGE_MOVE_IN = "move_in";
    public static final String STAGE_MOVE_OUT = "move_out";

    @TableId
    private String id;

    private String contractId;

    private String roomCode;

    private String itemCode;

    /** move_in 或 move_out */
    private String stage;

    private String url;

    private String userId;

    private LocalDateTime capturedAt;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
