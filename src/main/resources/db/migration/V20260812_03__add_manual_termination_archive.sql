ALTER TABLE lease_termination_applications
    ADD COLUMN termination_mode VARCHAR(20) NULL COMMENT 'ESIGN/MANUAL' AFTER rescission_status,
    ADD COLUMN manual_termination_reason VARCHAR(1000) NULL COMMENT '管理端线下解约归档原因' AFTER termination_mode,
    ADD COLUMN manual_agreement_urls TEXT NULL COMMENT '线下解约协议或凭证URL(JSON)' AFTER manual_termination_reason,
    ADD COLUMN manual_completed_by VARCHAR(36) NULL COMMENT '线下解约确认管理员ID' AFTER manual_agreement_urls,
    ADD COLUMN manual_completed_at DATETIME NULL COMMENT '线下解约确认时间' AFTER manual_completed_by;
