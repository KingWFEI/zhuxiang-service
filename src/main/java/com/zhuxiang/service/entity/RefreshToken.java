package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户刷新令牌表
 * @TableName refresh_token
 */
@TableName(value ="refresh_token")
@Data
public class RefreshToken implements Serializable {
    /**
     * 刷新令牌ID，主键
     */
    @TableId
    private String id;

    /**
     * 所属用户ID
     */
    private String userId;

    /**
     * 刷新令牌字符串
     */
    private String refreshToken;

    /**
     * 刷新令牌过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 是否已撤销：0未撤销，1已撤销
     */
    private Integer revoked;

    /**
     * 刷新令牌撤销时间
     */
    private LocalDateTime revokedAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

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
        RefreshToken other = (RefreshToken) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getRefreshToken() == null ? other.getRefreshToken() == null : this.getRefreshToken().equals(other.getRefreshToken()))
            && (this.getExpiresAt() == null ? other.getExpiresAt() == null : this.getExpiresAt().equals(other.getExpiresAt()))
            && (this.getRevoked() == null ? other.getRevoked() == null : this.getRevoked().equals(other.getRevoked()))
            && (this.getRevokedAt() == null ? other.getRevokedAt() == null : this.getRevokedAt().equals(other.getRevokedAt()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getRefreshToken() == null) ? 0 : getRefreshToken().hashCode());
        result = prime * result + ((getExpiresAt() == null) ? 0 : getExpiresAt().hashCode());
        result = prime * result + ((getRevoked() == null) ? 0 : getRevoked().hashCode());
        result = prime * result + ((getRevokedAt() == null) ? 0 : getRevokedAt().hashCode());
        result = prime * result + ((getCreatedAt() == null) ? 0 : getCreatedAt().hashCode());
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
        sb.append(", refreshToken=").append(refreshToken);
        sb.append(", expiresAt=").append(expiresAt);
        sb.append(", revoked=").append(revoked);
        sb.append(", revokedAt=").append(revokedAt);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}