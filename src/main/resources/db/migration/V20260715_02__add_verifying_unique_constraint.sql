-- 防止同一用户同时存在多个 VERIFYING 认证记录
-- MySQL 8.0.13+ 支持函数索引，NULL 值在唯一索引中不冲突
ALTER TABLE user_real_name_auth
    ADD UNIQUE INDEX uk_user_verifying ((CASE WHEN auth_status = 'VERIFYING' THEN user_id ELSE NULL END));
