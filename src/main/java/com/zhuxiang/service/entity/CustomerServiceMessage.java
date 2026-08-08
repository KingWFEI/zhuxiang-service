package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客服消息表
 */
@TableName(value = "customer_service_message")
@Data
public class CustomerServiceMessage implements Serializable {

    @TableId
    private String id;

    /** 所属会话ID */
    private String sessionId;

    /** 消息角色：USER ASSISTANT SYSTEM */
    private String role;

    /** 消息文本内容 */
    private String content;

    /** 扩展元数据JSON */
    private String metadataJson;

    /** 消息状态：SENT STREAMING DONE FAILED */
    private String status;

    /** 失败原因，仅服务端记录。 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
