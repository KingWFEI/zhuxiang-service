package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈表
 */
@TableName(value = "customer_service_feedback")
@Data
public class CustomerServiceFeedback implements Serializable {

    @TableId
    private String id;

    /** 被评价的消息ID */
    private String messageId;

    /** 所属会话ID */
    private String sessionId;

    /** 反馈用户ID */
    private String userId;

    /** 反馈类型：LIKE DISLIKE */
    private String feedbackType;

    /** 反馈备注 */
    private String comment;

    /** 创建时间 */
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
