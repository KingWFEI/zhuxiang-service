package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户消息表
 * @TableName message
 */
@TableName(value ="message")
@Data
public class Message implements Serializable {
    /**
     * 消息ID，主键
     */
    @TableId
    private String id;

    /**
     * 接收消息的用户ID
     */
    private String userId;

    /**
     * 消息分类：system系统，lease租约，lock门锁，bill账单，appointment预约，repair报修
     */
    private String category;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息正文内容
     */
    private String content;

    /**
     * 前端图标Key
     */
    private String iconKey;

    /**
     * 消息动作类型：none无动作，house房源，lease租约，bill账单，lock门锁，repair报修，appointment预约，url链接
     */
    private String actionType;

    /**
     * 消息动作目标，例如业务ID或URL
     */
    private String actionTarget;

    /**
     * 是否已读：0未读，1已读
     */
    private Integer isRead;

    /**
     * 是否删除：0未删除，1已删除
     */
    private Integer isDeleted;

    /**
     * 消息已读时间
     */
    private LocalDateTime readAt;

    /**
     * 消息创建时间
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
        Message other = (Message) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getCategory() == null ? other.getCategory() == null : this.getCategory().equals(other.getCategory()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
            && (this.getIconKey() == null ? other.getIconKey() == null : this.getIconKey().equals(other.getIconKey()))
            && (this.getActionType() == null ? other.getActionType() == null : this.getActionType().equals(other.getActionType()))
            && (this.getActionTarget() == null ? other.getActionTarget() == null : this.getActionTarget().equals(other.getActionTarget()))
            && (this.getIsRead() == null ? other.getIsRead() == null : this.getIsRead().equals(other.getIsRead()))
            && (this.getIsDeleted() == null ? other.getIsDeleted() == null : this.getIsDeleted().equals(other.getIsDeleted()))
            && (this.getReadAt() == null ? other.getReadAt() == null : this.getReadAt().equals(other.getReadAt()))
            && (this.getCreatedAt() == null ? other.getCreatedAt() == null : this.getCreatedAt().equals(other.getCreatedAt()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getIconKey() == null) ? 0 : getIconKey().hashCode());
        result = prime * result + ((getActionType() == null) ? 0 : getActionType().hashCode());
        result = prime * result + ((getActionTarget() == null) ? 0 : getActionTarget().hashCode());
        result = prime * result + ((getIsRead() == null) ? 0 : getIsRead().hashCode());
        result = prime * result + ((getIsDeleted() == null) ? 0 : getIsDeleted().hashCode());
        result = prime * result + ((getReadAt() == null) ? 0 : getReadAt().hashCode());
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
        sb.append(", category=").append(category);
        sb.append(", title=").append(title);
        sb.append(", content=").append(content);
        sb.append(", iconKey=").append(iconKey);
        sb.append(", actionType=").append(actionType);
        sb.append(", actionTarget=").append(actionTarget);
        sb.append(", isRead=").append(isRead);
        sb.append(", isDeleted=").append(isDeleted);
        sb.append(", readAt=").append(readAt);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}