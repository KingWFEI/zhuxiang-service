-- =====================================================
-- 退租验收标准 & 租约验收快照
-- =====================================================

-- 1. 房源验收模板（每房源一份，version 递增）
CREATE TABLE house_inspection_template
(
    id         VARCHAR(36) NOT NULL COMMENT '主键',
    house_id   VARCHAR(36) NOT NULL COMMENT '房源ID',
    version    INT         NOT NULL DEFAULT 1 COMMENT '模板版本号',
    rooms      JSON        NOT NULL COMMENT '房间与设施验收标准（JSON数组）',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_house_id (house_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='房源退租验收模板';

-- 2. 租约验收快照（签约时从模板复制，不可修改，后续只能追加更正）
CREATE TABLE lease_inspection_snapshot
(
    id                    VARCHAR(36)  NOT NULL COMMENT '主键',
    contract_id           VARCHAR(36)  NOT NULL COMMENT '合同ID',
    lease_id              VARCHAR(36)  NOT NULL COMMENT '租约ID',
    house_id              VARCHAR(36)  NOT NULL COMMENT '房源ID',
    template_version      INT          NOT NULL COMMENT '快照时的模板版本号',
    rooms                 JSON         NOT NULL COMMENT '房间与设施清单快照（JSON数组，不可变）',
    status                VARCHAR(30)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/TENANT_CONFIRMED/LANDLORD_CONFIRMED/LOCKED/DISPUTED/COMPLETED',
    move_in_submitted_at  DATETIME     DEFAULT NULL COMMENT '入住验收提交时间',
    move_in_submitted_by  VARCHAR(36)  DEFAULT NULL COMMENT '入住验收提交人',
    move_out_submitted_at DATETIME     DEFAULT NULL COMMENT '退租验收提交时间',
    move_out_submitted_by VARCHAR(36)  DEFAULT NULL COMMENT '退租验收提交人',
    completed_at          DATETIME     DEFAULT NULL COMMENT '验收完成时间',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_id (contract_id),
    KEY idx_lease_id (lease_id),
    KEY idx_house_id (house_id),
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='租约验收快照';

-- 3. 验收照片（每张照片关联合同+房间+设施+阶段）
CREATE TABLE inspection_photo
(
    id          VARCHAR(36)  NOT NULL COMMENT '主键',
    contract_id VARCHAR(36)  NOT NULL COMMENT '合同ID',
    room_code   VARCHAR(50)  NOT NULL COMMENT '房间编码',
    item_code   VARCHAR(50)  NOT NULL COMMENT '设施/验收项编码',
    stage       VARCHAR(20)  NOT NULL COMMENT 'move_in 或 move_out',
    url         VARCHAR(500) NOT NULL COMMENT '照片URL',
    user_id     VARCHAR(36)  NOT NULL COMMENT '上传人',
    captured_at DATETIME     DEFAULT NULL COMMENT '拍摄时间',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id),
    KEY idx_contract_id (contract_id),
    KEY idx_contract_stage (contract_id, stage),
    KEY idx_contract_room_item (contract_id, room_code, item_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='验收照片';

-- 4. 押金扣款明细（按验收项逐条记录）
CREATE TABLE deposit_deduction_item
(
    id                     VARCHAR(36)  NOT NULL COMMENT '主键',
    contract_id            VARCHAR(36)  NOT NULL COMMENT '合同ID',
    snapshot_id            VARCHAR(36)  NOT NULL COMMENT '验收快照ID',
    room_code              VARCHAR(50)  NOT NULL COMMENT '房间编码',
    item_code              VARCHAR(50)  NOT NULL COMMENT '设施/验收项编码',
    result                 VARCHAR(30)  NOT NULL COMMENT 'UNCHANGED/NORMAL_WEAR/NEW_DAMAGE/MISSING/DISPUTED',
    reason                 VARCHAR(500) DEFAULT NULL COMMENT '扣款原因',
    deduction_amount       INT          NOT NULL DEFAULT 0 COMMENT '扣款金额（分）',
    evidence_urls          JSON         DEFAULT NULL COMMENT '证据照片URL列表',
    tenant_status          VARCHAR(20)  DEFAULT NULL COMMENT 'ACCEPT/DISPUTE',
    tenant_dispute_reason  VARCHAR(500) DEFAULT NULL COMMENT '租客异议原因',
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_snapshot_id (snapshot_id),
    KEY idx_contract_id (contract_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='押金扣款明细';
