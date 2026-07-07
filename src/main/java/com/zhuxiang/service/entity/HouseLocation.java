package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房源位置信息表。
 */
@Data
@TableName("house_location")
public class HouseLocation {

    @TableId
    private String id;

    private String houseId;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String province;

    private String city;

    private String district;

    private String township;

    private String neighborhood;

    private String address;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
