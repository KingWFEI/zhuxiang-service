package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客服会话表
 */
@TableName(value = "customer_service_session")
@Data
public class CustomerServiceSession implements Serializable {

    @TableId
    private String id;

    /** 用户ID */
    private String userId;

    /** 会话标题 */
    private String title;

    /** 会话状态：ACTIVE活跃中 CLOSED已关闭 */
    private String status;

    /** 消息总数 */
    private Integer messageCount;

    /** 最后一条消息预览 */
    private String lastMessagePreview;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 软删除时间 */
    private LocalDateTime deletedAt;

    /** 关闭原因：TIMEOUT超时归档 USER_NEW_SESSION用户新建 USER_CLOSED用户关闭 SYSTEM_ERROR异常 */
    private String closedReason;

    /** 最后一条消息时间 */
    private LocalDateTime lastMessageAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
