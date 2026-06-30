package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "repair_record")
@Data
public class RepairRecord implements Serializable {

    @TableId
    private String id;

    private String orderNo;

    private String userId;

    private String houseId;

    private String houseName;

    private String roomName;

    private String repairType;

    private String description;

    private String imageUrls;

    private String contactName;

    private String contactPhone;

    private LocalDateTime expectedVisitTime;

    private String status;

    private String housekeeperName;

    private String housekeeperPhone;

    private String repairmanName;

    private String assignee;

    private Integer rating;

    private String reviewContent;

    private LocalDateTime reviewTime;

    private String cancelReason;

    private LocalDateTime cancelTime;

    private LocalDateTime completedTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
