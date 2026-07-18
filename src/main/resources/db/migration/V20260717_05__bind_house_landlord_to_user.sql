-- house.landlord_id 的新语义统一为平台用户 user.id。
-- 先修复历史 landlord.id 数据，再建立数据库级引用约束。

-- 优先按照相同手机号补齐旧 landlord 与平台用户的关系。
UPDATE landlord l
INNER JOIN `user` u ON u.phone = l.phone
SET l.user_id = u.id
WHERE l.user_id IS NULL;

-- 已经绑定平台用户的旧房东，其房源切换为对应 user.id。
UPDATE house h
INNER JOIN landlord l ON h.landlord_id = l.id
SET h.landlord_id = l.user_id
WHERE l.user_id IS NOT NULL;

-- 为尚未迁移且没有平台账号的历史/演示房东创建不可登录的占位用户。
-- 使用原 landlord.id 作为 user.id，可在不丢失房源的前提下满足外键约束。
INSERT INTO `user` (
    id, phone, password_hash, nickname, avatar_url, role,
    is_verified, status, last_login_at, created_at, updated_at
)
SELECT DISTINCT
    h.landlord_id,
    CONCAT('L', LEFT(SHA2(h.landlord_id, 256), 19)),
    NULL,
    LEFT(COALESCE(NULLIF(l.name, ''), '历史房东'), 30),
    COALESCE(l.avatar_url, ''),
    'LANDLORD',
    COALESCE(l.is_verified, 0),
    'inactive',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM house h
LEFT JOIN `user` u ON u.id = h.landlord_id
LEFT JOIN landlord l ON l.id = h.landlord_id
WHERE u.id IS NULL;

-- 同步历史房东记录的用户映射，便于后续认领和迁移。
UPDATE landlord l
INNER JOIN `user` u ON u.id = l.id
SET l.user_id = u.id
WHERE l.user_id IS NULL;

CREATE INDEX idx_house_landlord_id ON house (landlord_id);

ALTER TABLE house
    ADD CONSTRAINT fk_house_landlord_user
        FOREIGN KEY (landlord_id) REFERENCES `user` (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT;
