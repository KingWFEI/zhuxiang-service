package com.zhuxiang.service.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * LLM调用日志表
 */
@TableName(value = "customer_service_llm_log")
@Data
public class CustomerServiceLlmLog implements Serializable {

    @TableId
    private String id;

    /** 所属会话ID */
    private String sessionId;

    /** 关联的assistant消息ID */
    private String messageId;

    /** 模型名称 */
    private String model;

    /** 输入Token数 */
    private Integer promptTokens;

    /** 输出Token数 */
    private Integer completionTokens;

    /** Token总数 */
    private Integer totalTokens;

    /** 调用耗时(毫秒) */
    private Integer latencyMs;

    /** 识别到的意图 */
    private String intent;

    /** 是否需要人工介入：0否 1是 */
    private Integer needHuman;

    /** 调用失败原因 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
