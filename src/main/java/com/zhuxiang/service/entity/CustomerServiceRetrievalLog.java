package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 检索日志表
 */
@TableName(value = "customer_service_retrieval_log")
@Data
public class CustomerServiceRetrievalLog implements Serializable {

    @TableId
    private String id;

    /** 所属会话ID */
    private String sessionId;

    /** 触发检索的消息ID */
    private String messageId;

    /** 检索查询文本 */
    private String queryText;

    /** 检索返回条数 */
    private Integer topK;

    /** 检索结果摘要JSON */
    private String retrievedChunks;

    /** 检索耗时(毫秒) */
    private Integer retrievalDurationMs;

    /** 创建时间 */
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
