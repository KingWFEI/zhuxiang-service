-- 退租流程改为以租约为业务入口；保留 contract_id 兼容历史数据和合同展示。
-- MySQL DDL 会自动提交；使用 information_schema 保证迁移失败后可以安全重跑。
SET @termination_lease_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lease_termination_applications'
      AND COLUMN_NAME = 'lease_id'
);
SET @termination_lease_column_sql = IF(
    @termination_lease_column_exists = 0,
    'ALTER TABLE lease_termination_applications ADD COLUMN lease_id VARCHAR(36) NULL COMMENT ''关联租约ID'' AFTER tenant_id',
    'SELECT 1'
);
PREPARE termination_lease_column_stmt FROM @termination_lease_column_sql;
EXECUTE termination_lease_column_stmt;
DEALLOCATE PREPARE termination_lease_column_stmt;

SET @termination_lease_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lease_termination_applications'
      AND INDEX_NAME = 'idx_lease_status'
);
SET @termination_lease_index_sql = IF(
    @termination_lease_index_exists = 0,
    'ALTER TABLE lease_termination_applications ADD KEY idx_lease_status (lease_id, status)',
    'SELECT 1'
);
PREPARE termination_lease_index_stmt FROM @termination_lease_index_sql;
EXECUTE termination_lease_index_stmt;
DEALLOCATE PREPARE termination_lease_index_stmt;

-- 根据旧申请的合同关联回填租约ID。历史孤立数据保持 NULL，避免迁移阻断启动。
UPDATE lease_termination_applications application
JOIN lease ON lease.contract_id COLLATE utf8mb4_general_ci = application.contract_id
SET application.lease_id = lease.id
WHERE application.lease_id IS NULL;

ALTER TABLE lock_permission
    MODIFY COLUMN status VARCHAR(20) NOT NULL
        COMMENT '权限状态：ACTIVE生效/FAILED下发失败/REVOKED已撤销/REVOKE_FAILED撤销失败/EXPIRED已过期';
