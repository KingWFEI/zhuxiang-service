package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 房源退租验收模板。每房源一份，version 每次保存递增。
 */
@TableName("house_inspection_template")
@Data
public class HouseInspectionTemplate implements Serializable {

    @TableId
    private String id;

    private String houseId;

    private Integer version;

    /** JSON 数组：[{roomCode, roomName, items: [{itemCode, itemName, required, minPhotoCount, remarkRequired, instruction, enabled}]}] */
    private String rooms;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
