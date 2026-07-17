-- =====================================================
-- 验收快照增加审批字段
-- =====================================================
ALTER TABLE lease_inspection_snapshot
    ADD COLUMN reviewed_by     VARCHAR(36)  DEFAULT NULL COMMENT '审批人用户ID' AFTER status,
    ADD COLUMN reviewed_at     DATETIME     DEFAULT NULL COMMENT '审批时间' AFTER reviewed_by,
    ADD COLUMN review_action   VARCHAR(20)  DEFAULT NULL COMMENT 'APPROVE/REJECT' AFTER reviewed_at,
    ADD COLUMN review_comment  VARCHAR(500) DEFAULT NULL COMMENT '审批意见或驳回原因' AFTER review_action;
