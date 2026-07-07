-- 客服会话表
CREATE TABLE customer_service_session (
    id VARCHAR(36) NOT NULL COMMENT '主键ID',
    user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    title VARCHAR(100) DEFAULT NULL COMMENT '会话标题，取用户第一句话前20个中文字符',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态：ACTIVE活跃中 CLOSED已关闭',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息总数',
    last_message_preview VARCHAR(200) DEFAULT NULL COMMENT '最后一条消息预览',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    KEY idx_cs_session_user (user_id),
    KEY idx_cs_session_updated (updated_at),
    KEY idx_cs_session_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客服会话表';

-- 客服消息表
CREATE TABLE customer_service_message (
    id VARCHAR(36) NOT NULL COMMENT '主键ID',
    session_id VARCHAR(36) NOT NULL COMMENT '所属会话ID',
    role VARCHAR(16) NOT NULL COMMENT '消息角色：USER用户 ASSISTANT客服AI SYSTEM系统',
    content TEXT DEFAULT NULL COMMENT '消息文本内容',
    metadata_json JSON DEFAULT NULL COMMENT '扩展元数据：知识片段引用、工具调用结果等',
    status VARCHAR(20) NOT NULL DEFAULT 'SENT' COMMENT '消息状态：SENT已发送 STREAMING流式输出中 DONE输出完成 FAILED失败',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_cs_msg_session (session_id),
    KEY idx_cs_msg_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客服消息表';

-- 知识库文档表
CREATE TABLE customer_service_kb_document (
    id VARCHAR(36) NOT NULL COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    category VARCHAR(50) DEFAULT NULL COMMENT '文档分类：PLATFORM_RULE平台规则 APP_USAGE使用帮助 LOCK锁 FAQ等',
    original_filename VARCHAR(500) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(10) NOT NULL COMMENT '文件类型：PDF DOCX TXT MD',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    file_path VARCHAR(1000) NOT NULL COMMENT '存储路径',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '向量分块数量',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待处理 PROCESSING处理中 ACTIVE已启用 DISABLED已停用 FAILED失败',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '处理失败原因',
    vectorize_failed_count INT NOT NULL DEFAULT 0 COMMENT '向量化失败重试次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    KEY idx_cs_kb_status (status),
    KEY idx_cs_kb_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库文档表';

-- 检索日志表
CREATE TABLE customer_service_retrieval_log (
    id VARCHAR(36) NOT NULL COMMENT '主键ID',
    session_id VARCHAR(36) NOT NULL COMMENT '所属会话ID',
    message_id VARCHAR(36) DEFAULT NULL COMMENT '触发检索的消息ID',
    query_text VARCHAR(500) NOT NULL COMMENT '检索查询文本',
    top_k INT NOT NULL DEFAULT 5 COMMENT '检索返回条数',
    retrieved_chunks JSON DEFAULT NULL COMMENT '检索结果摘要：chunkId、documentId、score、chunkPreview',
    retrieval_duration_ms INT DEFAULT NULL COMMENT '检索耗时(毫秒)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_cs_retrieval_session (session_id),
    KEY idx_cs_retrieval_msg (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检索日志表';

-- LLM调用日志表
CREATE TABLE customer_service_llm_log (
    id VARCHAR(36) NOT NULL COMMENT '主键ID',
    session_id VARCHAR(36) NOT NULL COMMENT '所属会话ID',
    message_id VARCHAR(36) DEFAULT NULL COMMENT '关联的assistant消息ID',
    model VARCHAR(50) NOT NULL COMMENT '模型名称，如deepseek-v4-pro',
    prompt_tokens INT DEFAULT NULL COMMENT '输入Token数',
    completion_tokens INT DEFAULT NULL COMMENT '输出Token数',
    total_tokens INT DEFAULT NULL COMMENT 'Token总数',
    latency_ms INT DEFAULT NULL COMMENT '调用耗时(毫秒)',
    intent VARCHAR(50) DEFAULT NULL COMMENT '识别到的意图',
    need_human TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要人工介入：0否 1是',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '调用失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_cs_llm_session (session_id),
    KEY idx_cs_llm_msg (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='LLM调用日志表';

-- 用户反馈表
CREATE TABLE customer_service_feedback (
    id VARCHAR(36) NOT NULL COMMENT '主键ID',
    message_id VARCHAR(36) NOT NULL COMMENT '被评价的消息ID',
    session_id VARCHAR(36) NOT NULL COMMENT '所属会话ID',
    user_id VARCHAR(36) NOT NULL COMMENT '反馈用户ID',
    feedback_type VARCHAR(16) NOT NULL COMMENT '反馈类型：LIKE点赞 DISLIKE点踩',
    comment VARCHAR(500) DEFAULT NULL COMMENT '反馈备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cs_feedback_msg_user (message_id, user_id),
    KEY idx_cs_feedback_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户反馈表';
