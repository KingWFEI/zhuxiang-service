-- Real-name verification is authoritative in user_real_name_auth.auth_status.
-- Keeping the same state in user.is_verified caused the two values to diverge.
ALTER TABLE `user`
    DROP COLUMN is_verified;
