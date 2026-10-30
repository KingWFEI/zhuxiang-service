CREATE TABLE IF NOT EXISTS app_legal_document (
    document_type VARCHAR(32) NOT NULL COMMENT '文档类型：USER_AGREEMENT、PRIVACY_POLICY',
    title VARCHAR(100) NOT NULL COMMENT '文档标题',
    content_markdown MEDIUMTEXT NOT NULL COMMENT 'Markdown 正文',
    updated_by VARCHAR(36) NULL COMMENT '最后更新管理员 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APP 用户协议与隐私政策';

INSERT IGNORE INTO app_legal_document
    (document_type, title, content_markdown, updated_by, created_at, updated_at)
VALUES
    ('USER_AGREEMENT', '用户协议', '', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PRIVACY_POLICY', '隐私政策', '', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
