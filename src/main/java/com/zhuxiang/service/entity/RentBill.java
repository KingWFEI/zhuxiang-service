package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName(value = "rent_bill")
@Data
public class RentBill implements Serializable {

    @TableId
    private String id;

    private String leaseId;

    private Integer periodNo;

    private Integer amountDue;

    private Integer amountPaid;

    private LocalDate dueDate;

    private LocalDateTime paidAt;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
