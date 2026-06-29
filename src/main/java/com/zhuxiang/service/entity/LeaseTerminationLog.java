package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "lease_termination_logs")
@Data
public class LeaseTerminationLog implements Serializable {

    @TableId
    private String id;

    private String applicationId;

    private String action;

    private String fromStatus;

    private String toStatus;

    private String operatorId;

    private String operatorName;

    private String remark;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
