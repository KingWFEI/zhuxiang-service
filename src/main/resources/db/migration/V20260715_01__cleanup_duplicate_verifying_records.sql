-- ============================================================
-- 清理同一 user_id 下已存在的重复 VERIFYING 记录
-- ============================================================
-- 背景：在创建条件唯一索引之前，先清理历史数据中已存在的重复
--       VERIFYING 记录，确保后续 V20260715_02 建索引不会失败。
--
-- 策略：对于同一 user_id 存在多条 auth_status = 'VERIFYING' 的情况，
--       保留 created_at 最新的一条，其余标记为 EXPIRED。
--       如果没有重复数据，本迁移为空操作。
-- ============================================================

UPDATE user_real_name_auth t1
    INNER JOIN (
        SELECT user_id, MAX(created_at) AS max_created
        FROM user_real_name_auth
        WHERE auth_status = 'VERIFYING'
        GROUP BY user_id
        HAVING COUNT(*) > 1
    ) t2 ON t1.user_id = t2.user_id AND t1.auth_status = 'VERIFYING'
SET t1.auth_status = 'EXPIRED',
    t1.updated_at = NOW()
WHERE t1.created_at < t2.max_created;
