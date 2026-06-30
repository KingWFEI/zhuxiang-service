-- 退租流程改为以租约为业务入口；保留 contract_id 兼容历史数据和合同展示。
ALTER TABLE lease_termination_applications
    ADD COLUMN lease_id VARCHAR(36) NULL COMMENT '关联租约ID' AFTER tenant_id,
    ADD KEY idx_lease_status (lease_id, status);

-- 根据旧申请的合同关联回填租约ID。历史孤立数据保持 NULL，避免迁移阻断启动。
UPDATE lease_termination_applications application
JOIN lease ON lease.contract_id = application.contract_id
SET application.lease_id = lease.id
WHERE application.lease_id IS NULL;

ALTER TABLE lock_permission
    MODIFY COLUMN status VARCHAR(20) NOT NULL
        COMMENT '权限状态：ACTIVE生效/FAILED下发失败/REVOKED已撤销/REVOKE_FAILED撤销失败/EXPIRED已过期';
