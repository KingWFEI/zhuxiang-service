-- =====================================================
-- 验收快照增加验房完成人、完成备注字段
-- 简化退租验收流程：取消驳回审批，由管理端直接锁定
-- =====================================================
ALTER TABLE lease_inspection_snapshot
    ADD COLUMN completed_by       VARCHAR(36)  DEFAULT NULL COMMENT '验房完成人用户ID' AFTER completed_at,
    ADD COLUMN completion_comment VARCHAR(500) DEFAULT NULL COMMENT '验房完成备注' AFTER completed_by;
