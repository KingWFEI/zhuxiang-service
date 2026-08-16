-- Align the inspection tables before V20260812_04 compares their identifiers
-- with lease. On databases that already ran V20260814_01, this migration is
-- applied out of order and the guards intentionally make it a no-op.
SET @ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'house_inspection_template'
          AND table_collation <> 'utf8mb4_0900_ai_ci'
    ),
    'ALTER TABLE house_inspection_template CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'lease_inspection_snapshot'
          AND table_collation <> 'utf8mb4_0900_ai_ci'
    ),
    'ALTER TABLE lease_inspection_snapshot CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
