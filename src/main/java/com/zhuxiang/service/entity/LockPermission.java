package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 门锁权限表
 * @TableName lock_permission
 */
@TableName(value ="lock_permission")
@Data
public class LockPermission implements Serializable {
    /**
     * 门锁权限ID，主键
     */
    @TableId
    private String id;

    /**
     * 拥有权限的用户ID
     */
    private String userId;

    /**
     * 关联租约ID
     */
    private String leaseId;

    /**
     * 关联门锁ID
     */
    private String lockId;

    /**
     * 权限状态：active有效，expired已过期，revoked已回收
     */
    private String status;

    /**
     * 权限开始时间
     */
    private LocalDateTime validFrom;

    /**
     * 权限结束时间
     */
    private LocalDateTime validTo;

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
        LockPermission other = (LockPermission) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getLeaseId() == null ? other.getLeaseId() == null : this.getLeaseId().equals(other.getLeaseId()))
            && (this.getLockId() == null ? other.getLockId() == null : this.getLockId().equals(other.getLockId()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getValidFrom() == null ? other.getValidFrom() == null : this.getValidFrom().equals(other.getValidFrom()))
            && (this.getValidTo() == null ? other.getValidTo() == null : this.getValidTo().equals(other.getValidTo()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getLeaseId() == null) ? 0 : getLeaseId().hashCode());
        result = prime * result + ((getLockId() == null) ? 0 : getLockId().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getValidFrom() == null) ? 0 : getValidFrom().hashCode());
        result = prime * result + ((getValidTo() == null) ? 0 : getValidTo().hashCode());
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
        sb.append(", userId=").append(userId);
        sb.append(", leaseId=").append(leaseId);
        sb.append(", lockId=").append(lockId);
        sb.append(", status=").append(status);
        sb.append(", validFrom=").append(validFrom);
        sb.append(", validTo=").append(validTo);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}