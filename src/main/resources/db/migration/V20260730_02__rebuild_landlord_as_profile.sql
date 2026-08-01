-- landlord 不再表示房源归属主体，而是 LANDLORD 用户的一对一公开资料。
-- house.landlord_id 继续引用 user.id；landlord.id 仅作为资料记录主键。

INSERT INTO `user` (
    id, phone, password_hash, nickname, avatar_url, role,
    is_verified, status, last_login_at, created_at, updated_at
)
SELECT
    l.id,
    CONCAT('P', LEFT(SHA2(CONCAT('landlord-profile:', l.id), 256), 19)),
    NULL,
    LEFT(COALESCE(NULLIF(l.name, ''), '历史房东'), 30),
    COALESCE(l.avatar_url, ''),
    'LANDLORD',
    COALESCE(l.is_verified, 0),
    'inactive',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM landlord l
LEFT JOIN `user` u ON u.id = l.id
WHERE l.user_id IS NULL
  AND u.id IS NULL;

UPDATE landlord l
INNER JOIN `user` u ON u.id = l.id
SET l.user_id = u.id
WHERE l.user_id IS NULL;

DELETE duplicate_profile
FROM landlord duplicate_profile
INNER JOIN landlord kept_profile
        ON kept_profile.user_id = duplicate_profile.user_id
       AND (
            kept_profile.created_at < duplicate_profile.created_at
            OR (
                kept_profile.created_at = duplicate_profile.created_at
                AND kept_profile.id < duplicate_profile.id
            )
       )
WHERE duplicate_profile.user_id IS NOT NULL;

INSERT INTO landlord (
    id, user_id, name, avatar_url, phone, is_verified, rating, rented_count,
    response_description, created_at, updated_at
)
SELECT
    u.id,
    u.id,
    LEFT(COALESCE(NULLIF(u.nickname, ''), '房东'), 50),
    COALESCE(u.avatar_url, ''),
    u.phone,
    COALESCE(u.is_verified, 0),
    0,
    0,
    '通常会及时回复',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM `user` u
LEFT JOIN landlord l ON l.user_id = u.id
WHERE u.role = 'LANDLORD'
  AND l.id IS NULL;

ALTER TABLE landlord
    ADD COLUMN cover_image_url VARCHAR(500) NULL COMMENT '公开主页封面图URL' AFTER avatar_url,
    ADD COLUMN slogan VARCHAR(120) NULL COMMENT '一句话个性签名' AFTER cover_image_url,
    ADD COLUMN introduction VARCHAR(1000) NULL COMMENT '房东个人介绍' AFTER slogan,
    ADD COLUMN service_area VARCHAR(200) NULL COMMENT '主要服务区域' AFTER introduction,
    ADD COLUMN service_years INT NOT NULL DEFAULT 0 COMMENT '从业或出租服务年限' AFTER service_area,
    ADD COLUMN profile_tags VARCHAR(500) NULL COMMENT '服务标签，换行分隔' AFTER service_years,
    ADD COLUMN wechat VARCHAR(100) NULL COMMENT '微信号' AFTER phone,
    ADD COLUMN email VARCHAR(150) NULL COMMENT '联系邮箱' AFTER wechat,
    ADD COLUMN contact_time VARCHAR(100) NULL COMMENT '方便联系的时间说明' AFTER email,
    ADD COLUMN show_phone TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开手机号' AFTER contact_time,
    ADD COLUMN show_wechat TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开微信号' AFTER show_phone,
    ADD COLUMN show_email TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开邮箱' AFTER show_wechat;

ALTER TABLE landlord
    DROP INDEX idx_landlord_user_id,
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '关联房东用户ID',
    MODIFY COLUMN name VARCHAR(50) NOT NULL COMMENT '公开展示名称',
    MODIFY COLUMN avatar_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '公开头像URL',
    ADD UNIQUE INDEX uk_landlord_user_id (user_id),
    ADD CONSTRAINT fk_landlord_user
        FOREIGN KEY (user_id) REFERENCES `user` (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    COMMENT = '房东公开资料表';
