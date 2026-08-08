package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;

/**
 * 房东公开资料表。
 *
 * <p>业务关联必须使用 {@code userId}；{@code id} 只是资料记录主键。</p>
 * @TableName landlord
 */
@TableName(value ="landlord")
@Data
public class Landlord implements Serializable {
    /**
     * 资料记录 ID
     */
    @TableId
    private String id;

    /**
     * 关联的房东用户 ID，一名房东仅有一份资料
     */
    private String userId;

    /**
     * 对租客展示的名称
     */
    private String name;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /** 公开主页封面图 URL。 */
    private String coverImageUrl;

    /** 一句话个性签名。 */
    private String slogan;

    /** 房东个人介绍。 */
    private String introduction;

    /** 主要服务区域。 */
    private String serviceArea;

    /** 从业或出租服务年限。 */
    private Integer serviceYears;

    /** 个性化服务标签，服务端以换行分隔存储。 */
    private String profileTags;

    /**
     * 联系电话，是否对租客公开由 showPhone 控制
     */
    private String phone;

    /** 微信号。 */
    private String wechat;

    /** 联系邮箱。 */
    private String email;

    /** 方便联系的时间说明。 */
    private String contactTime;

    /** 是否向租客公开手机号。 */
    private Integer showPhone;

    /** 是否向租客公开微信号。 */
    private Integer showWechat;

    /** 是否向租客公开邮箱。 */
    private Integer showEmail;

    /**
     * 历史身份证号密文，仅保留兼容，不得通过资料接口暴露
     */
    @ToString.Exclude
    private String idCardCiphertext;

    /**
     * 历史认证快照；对外展示以 user_real_name_auth 中的 VERIFIED 记录为准
     */
    private Integer isVerified;

    /**
     * 评分，范围建议0.0到5.0
     */
    private BigDecimal rating;

    /**
     * 累计出租房源数量
     */
    private Integer rentedCount;

    /**
     * 响应描述，例如回复及时
     */
    private String responseDescription;

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
        Landlord other = (Landlord) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getAvatarUrl() == null ? other.getAvatarUrl() == null : this.getAvatarUrl().equals(other.getAvatarUrl()))
            && (this.getPhone() == null ? other.getPhone() == null : this.getPhone().equals(other.getPhone()))
            && (this.getIsVerified() == null ? other.getIsVerified() == null : this.getIsVerified().equals(other.getIsVerified()))
            && (this.getRating() == null ? other.getRating() == null : this.getRating().equals(other.getRating()))
            && (this.getRentedCount() == null ? other.getRentedCount() == null : this.getRentedCount().equals(other.getRentedCount()))
            && (this.getResponseDescription() == null ? other.getResponseDescription() == null : this.getResponseDescription().equals(other.getResponseDescription()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()))
            && (this.getUpdatedAt() == null ? other.getUpdatedAt() == null : this.getUpdatedAt().equals(other.getUpdatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getAvatarUrl() == null) ? 0 : getAvatarUrl().hashCode());
        result = prime * result + ((getPhone() == null) ? 0 : getPhone().hashCode());
        result = prime * result + ((getIsVerified() == null) ? 0 : getIsVerified().hashCode());
        result = prime * result + ((getRating() == null) ? 0 : getRating().hashCode());
        result = prime * result + ((getRentedCount() == null) ? 0 : getRentedCount().hashCode());
        result = prime * result + ((getResponseDescription() == null) ? 0 : getResponseDescription().hashCode());
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
        sb.append(", name=").append(name);
        sb.append(", avatarUrl=").append(avatarUrl);
        sb.append(", phone=").append(phone);
        sb.append(", isVerified=").append(isVerified);
        sb.append(", rating=").append(rating);
        sb.append(", rentedCount=").append(rentedCount);
        sb.append(", responseDescription=").append(responseDescription);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
