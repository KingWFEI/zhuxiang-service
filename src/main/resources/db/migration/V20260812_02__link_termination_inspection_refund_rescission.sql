ALTER TABLE lease_termination_applications
    ADD COLUMN recommended_refund_amount INT NULL COMMENT '系统建议退款金额，单位分' AFTER refund_amount,
    ADD COLUMN refund_adjustment_reason VARCHAR(1000) NULL COMMENT '管理端调整退款金额原因' AFTER recommended_refund_amount,
    ADD COLUMN settlement_operator_id VARCHAR(36) NULL COMMENT '结算操作人' AFTER refund_adjustment_reason,
    ADD COLUMN rescission_sign_flow_id VARCHAR(64) NULL COMMENT 'e签宝解约签署流程ID' AFTER settlement_operator_id,
    ADD COLUMN rescission_status VARCHAR(32) NULL COMMENT '解约状态：pending/signing/completed/failed' AFTER rescission_sign_flow_id,
    ADD COLUMN rescission_started_at DATETIME NULL AFTER rescission_status,
    ADD COLUMN rescission_completed_at DATETIME NULL AFTER rescission_started_at,
    ADD COLUMN process_last_error VARCHAR(1000) NULL AFTER rescission_completed_at,
    ADD COLUMN process_retry_at DATETIME NULL AFTER process_last_error,
    ADD UNIQUE KEY uk_termination_rescission_flow (rescission_sign_flow_id),
    ADD KEY idx_termination_process (status, process_retry_at);

UPDATE lease_termination_applications
SET status = 'pending_photos', updated_at = CURRENT_TIMESTAMP
WHERE status IN ('pending_review', 'need_supplement', 'approved');

UPDATE lease_termination_applications application
JOIN lease_inspection_snapshot snapshot
    ON snapshot.contract_id COLLATE utf8mb4_general_ci = application.contract_id
SET application.status = CASE
        WHEN snapshot.status = 'LOCKED' THEN 'settlement_pending'
        WHEN snapshot.status = 'SUBMITTED' THEN 'inspection_pending'
        ELSE application.status
    END,
    application.inspection_completed_time = CASE
        WHEN snapshot.status = 'LOCKED' THEN COALESCE(snapshot.completed_at, CURRENT_TIMESTAMP)
        ELSE application.inspection_completed_time
    END,
    application.updated_at = CURRENT_TIMESTAMP
WHERE application.status = 'pending_photos'
  AND snapshot.status IN ('SUBMITTED', 'LOCKED');
