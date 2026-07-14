package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "deposit_deduction")
@Data
public class DepositDeduction implements Serializable {

    @TableId
    private String id;

    private String depositRecordId;

    private String deductionType;

    private Integer amount;

    private String description;

    private String evidenceUrls;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
