DELIMITER //

CREATE PROCEDURE migrate_user_if_needed()
BEGIN
    -- 仅当 app_user 表存在时执行重命名
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'app_user'
    ) THEN
        ALTER TABLE app_user RENAME TO `user`;
    END IF;

    -- 仅当 role 列不存在时添加
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'role'
    ) THEN
        ALTER TABLE `user` ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'TENANT' AFTER avatar_url;
    END IF;

    -- 仅当旧约束存在时重建唯一约束
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = DATABASE() AND table_name = 'user'
        AND constraint_name = 'uk_app_user_phone'
    ) THEN
        ALTER TABLE `user` DROP INDEX uk_app_user_phone;
        ALTER TABLE `user` ADD CONSTRAINT uk_user_phone UNIQUE (phone);
    END IF;
END//

DELIMITER ;

CALL migrate_user_if_needed();
DROP PROCEDURE IF EXISTS migrate_user_if_needed;
