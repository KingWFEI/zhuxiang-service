CREATE TABLE esign_contract_template (
    id                      VARCHAR(36)  NOT NULL,
    business_type           VARCHAR(50)  NOT NULL,
    template_code           VARCHAR(100) NOT NULL,
    template_name           VARCHAR(200) NOT NULL,
    version                 INT          NOT NULL,
    environment             VARCHAR(20)  NOT NULL,
    doc_template_id         VARCHAR(64)  NULL,
    source_file_id          VARCHAR(64)  NULL,
    source_file_name        VARCHAR(255) NULL,
    template_type           INT          NOT NULL DEFAULT 0,
    status                  VARCHAR(20)  NOT NULL,
    component_fingerprint   VARCHAR(64)  NULL,
    esign_create_time       BIGINT       NULL,
    esign_update_time       BIGINT       NULL,
    last_synced_at          DATETIME     NULL,
    validation_status       VARCHAR(20)  NULL,
    validation_message      TEXT         NULL,
    version_note            VARCHAR(500) NULL,
    created_by              VARCHAR(36)  NULL,
    published_by            VARCHAR(36)  NULL,
    published_at            DATETIME     NULL,
    created_at              DATETIME     NOT NULL,
    updated_at              DATETIME     NOT NULL,
    version_lock            INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_esign_template_version (business_type, template_code, version),
    UNIQUE KEY uk_esign_doc_template_id (doc_template_id),
    KEY idx_esign_template_status (business_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='e签宝合同模板版本';

CREATE TABLE esign_template_component (
    id                    VARCHAR(36)  NOT NULL,
    template_id           VARCHAR(36)  NOT NULL,
    component_id          VARCHAR(64)  NOT NULL,
    component_key         VARCHAR(100) NULL,
    component_name        VARCHAR(200) NULL,
    component_type        INT          NULL,
    required_flag         TINYINT      NOT NULL DEFAULT 0,
    page_num              INT          NULL,
    position_x            DECIMAL(14,4) NULL,
    position_y            DECIMAL(14,4) NULL,
    component_width       DECIMAL(14,4) NULL,
    component_height      DECIMAL(14,4) NULL,
    signer_role           VARCHAR(50)  NULL,
    special_attribute     TEXT         NULL,
    mapping_mode          VARCHAR(30)  NULL,
    business_field_code   VARCHAR(100) NULL,
    fixed_value           TEXT         NULL,
    editable_flag         TINYINT      NOT NULL DEFAULT 0,
    sync_status           VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_esign_template_component (template_id, component_id),
    KEY idx_esign_component_key (template_id, component_key),
    CONSTRAINT fk_esign_component_template FOREIGN KEY (template_id)
        REFERENCES esign_contract_template (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='e签宝模板控件快照及业务映射';

CREATE TABLE esign_template_audit_log (
    id              VARCHAR(36)  NOT NULL,
    template_id     VARCHAR(36)  NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    operator_id     VARCHAR(36)  NULL,
    operator_name   VARCHAR(100) NULL,
    detail_text     TEXT         NULL,
    created_at      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_esign_audit_template_time (template_id, created_at),
    CONSTRAINT fk_esign_audit_template FOREIGN KEY (template_id)
        REFERENCES esign_contract_template (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同模板操作审计日志';

ALTER TABLE rent_contract
    ADD COLUMN template_config_id VARCHAR(36) NULL AFTER doc_template_id,
    ADD COLUMN template_version INT NULL AFTER template_config_id,
    ADD COLUMN template_fingerprint VARCHAR(64) NULL AFTER template_version,
    ADD KEY idx_rent_contract_template_config (template_config_id);
