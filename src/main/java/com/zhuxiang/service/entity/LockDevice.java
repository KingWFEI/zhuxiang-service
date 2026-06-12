package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 门锁设备表
 * @TableName lock_device
 */
@TableName(value ="lock_device")
@Data
public class LockDevice implements Serializable {
    /**
     * 门锁ID，主键
     */
    @TableId
    private String id;

    /**
     * 绑定房源ID
     */
    private String houseId;

    /**
     * 门锁名称，例如1201门锁
     */
    private String lockName;

    /**
     * 门锁品牌，默认通通锁
     */
    private String lockBrand;

    /**
     * 门锁序列号或厂商设备编号
     */
    private String lockSn;

    /**
     * 门锁状态：online在线，offline离线，low_battery低电量，unknown未知
     */
    private String status;

    /**
     * 门锁电量百分比
     */
    private Integer batteryLevel;

    /**
     * 网关ID，第二阶段接入gateway表后关联
     */
    private String gatewayId;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        LockDevice other = (LockDevice) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getHouseId() == null ? other.getHouseId() == null : this.getHouseId().equals(other.getHouseId()))
            && (this.getLockName() == null ? other.getLockName() == null : this.getLockName().equals(other.getLockName()))
            && (this.getLockBrand() == null ? other.getLockBrand() == null : this.getLockBrand().equals(other.getLockBrand()))
            && (this.getLockSn() == null ? other.getLockSn() == null : this.getLockSn().equals(other.getLockSn()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getBatteryLevel() == null ? other.getBatteryLevel() == null : this.getBatteryLevel().equals(other.getBatteryLevel()))
            && (this.getGatewayId() == null ? other.getGatewayId() == null : this.getGatewayId().equals(other.getGatewayId()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getHouseId() == null) ? 0 : getHouseId().hashCode());
        result = prime * result + ((getLockName() == null) ? 0 : getLockName().hashCode());
        result = prime * result + ((getLockBrand() == null) ? 0 : getLockBrand().hashCode());
        result = prime * result + ((getLockSn() == null) ? 0 : getLockSn().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getBatteryLevel() == null) ? 0 : getBatteryLevel().hashCode());
        result = prime * result + ((getGatewayId() == null) ? 0 : getGatewayId().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
        result = prime * result + ((getUpdatedAt() == null) ? 0 : getUpdatedAt().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", houseId=").append(houseId);
        sb.append(", lockName=").append(lockName);
        sb.append(", lockBrand=").append(lockBrand);
        sb.append(", lockSn=").append(lockSn);
        sb.append(", status=").append(status);
        sb.append(", batteryLevel=").append(batteryLevel);
        sb.append(", gatewayId=").append(gatewayId);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}