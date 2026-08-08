CREATE TABLE landlord_auth_application (
    id                       VARCHAR(36)  NOT NULL COMMENT '申请ID',
    application_no           VARCHAR(40)  NOT NULL COMMENT '申请编号',
    user_id                  VARCHAR(36)  NOT NULL COMMENT '申请用户ID',
    status                   VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/SUPERSEDED',
    real_name                VARCHAR(80)  NOT NULL COMMENT '真实姓名',
    id_card_ciphertext       VARCHAR(512) NOT NULL COMMENT '身份证号AES-GCM密文',
    id_card_masked           VARCHAR(32)  NOT NULL COMMENT '身份证号掩码',
    id_card_front_url        VARCHAR(1000) NOT NULL COMMENT '身份证人像面COS地址',
    id_card_back_url         VARCHAR(1000) NOT NULL COMMENT '身份证国徽面COS地址',
    contact_phone            VARCHAR(32)  NOT NULL COMMENT '联系电话',
    contact_wechat           VARCHAR(100) NULL COMMENT '微信号',
    contact_email            VARCHAR(160) NULL COMMENT '联系邮箱',
    contact_address          VARCHAR(500) NULL COMMENT '联系地址',
    preferred_contact_time   VARCHAR(160) NULL COMMENT '方便联系时间',
    applicant_note           VARCHAR(1000) NULL COMMENT '申请说明',
    reject_reason            VARCHAR(1000) NULL COMMENT '驳回原因',
    reviewer_id              VARCHAR(36) NULL COMMENT '审核人ID',
    reviewed_at              DATETIME NULL COMMENT '审核时间',
    created_at               DATETIME NOT NULL,
    updated_at               DATETIME NOT NULL,
    pending_user_id          VARCHAR(36) GENERATED ALWAYS AS
        (CASE WHEN status = 'PENDING' THEN user_id ELSE NULL END) STORED COMMENT '防止同一用户存在多个待审申请',
    PRIMARY KEY (id),
    UNIQUE KEY uk_landlord_auth_application_no (application_no),
    UNIQUE KEY uk_landlord_auth_pending_user (pending_user_id),
    KEY idx_landlord_auth_user_created (user_id, created_at),
    KEY idx_landlord_auth_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房东认证申请';

CREATE TABLE landlord_auth_proof (
    id               VARCHAR(36)  NOT NULL COMMENT '证明材料ID',
    application_id   VARCHAR(36)  NOT NULL COMMENT '房东认证申请ID',
    proof_type       VARCHAR(40)  NOT NULL COMMENT 'PROPERTY_CERTIFICATE/PURCHASE_CONTRACT/LEASE_CERTIFICATE/COURT_DECISION/OTHER',
    file_id          VARCHAR(36)  NOT NULL COMMENT '文件记录ID',
    file_url         VARCHAR(1000) NOT NULL COMMENT 'COS文件地址',
    created_at       DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_landlord_auth_proof_application (application_id),
    CONSTRAINT fk_landlord_auth_proof_application
        FOREIGN KEY (application_id) REFERENCES landlord_auth_application (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房东认证权属证明材料';
