CREATE TABLE `user` (
    id VARCHAR(36) PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100),
    nickname VARCHAR(30) NOT NULL,
    avatar_url VARCHAR(500) NOT NULL DEFAULT '',
    role VARCHAR(20) NOT NULL DEFAULT 'TENANT',
    is_verified TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_phone UNIQUE (phone)
);

CREATE TABLE sms_code (
    id VARCHAR(36) PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    scene VARCHAR(30) NOT NULL,
    code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used TINYINT NOT NULL DEFAULT 0,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_sms_code_phone_scene ON sms_code (phone, scene, created_at);

CREATE TABLE refresh_token (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    refresh_token VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked TINYINT NOT NULL DEFAULT 0,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_refresh_token_value UNIQUE (refresh_token)
);
CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

CREATE TABLE region (
    id VARCHAR(36) PRIMARY KEY,
    parent_id VARCHAR(36),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    level VARCHAR(30) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_region_code UNIQUE (code)
);

CREATE TABLE community (
    id VARCHAR(36) PRIMARY KEY,
    region_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(500),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_community_region ON community (region_id);

CREATE TABLE landlord (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(500) NOT NULL DEFAULT '',
    phone VARCHAR(20),
    is_verified TINYINT NOT NULL DEFAULT 0,
    rating DECIMAL(3, 2) NOT NULL DEFAULT 0,
    rented_count INT NOT NULL DEFAULT 0,
    response_description VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE house (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    cover_image VARCHAR(500) NOT NULL DEFAULT '',
    location VARCHAR(100) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    address VARCHAR(500),
    building VARCHAR(30),
    unit VARCHAR(30),
    room VARCHAR(30),
    price INT NOT NULL,
    deposit INT NOT NULL DEFAULT 0,
    payment_method VARCHAR(50),
    room_type VARCHAR(50),
    area DECIMAL(8, 2),
    floor VARCHAR(50),
    orientation VARCHAR(50),
    decoration VARCHAR(50),
    available_date DATE,
    metro VARCHAR(100),
    description TEXT,
    rent_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'available',
    is_smart_lock_supported TINYINT NOT NULL DEFAULT 0,
    is_self_viewing_supported TINYINT NOT NULL DEFAULT 0,
    landlord_id VARCHAR(36) NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    favorite_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_house_status_type ON house (status, rent_type);
CREATE INDEX idx_house_price ON house (price);
CREATE INDEX idx_house_community ON house (community_id);

CREATE TABLE house_image (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    image_type VARCHAR(30) NOT NULL DEFAULT 'normal',
    title VARCHAR(100),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_house_image_house ON house_image (house_id, sort_order);

CREATE TABLE house_tag (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    tag_type VARCHAR(30),
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE house_tag_relation (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    tag_id VARCHAR(36) NOT NULL,
    CONSTRAINT uk_house_tag_relation UNIQUE (house_id, tag_id)
);

CREATE TABLE house_facility (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    icon_key VARCHAR(50),
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE house_facility_relation (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    facility_id VARCHAR(36) NOT NULL,
    CONSTRAINT uk_house_facility_relation UNIQUE (house_id, facility_id)
);

CREATE TABLE advertisement (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    target_type VARCHAR(30) NOT NULL,
    target_value VARCHAR(500),
    position VARCHAR(30) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE user_favorite_house (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    house_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_favorite_house UNIQUE (user_id, house_id)
);
CREATE INDEX idx_favorite_user_time ON user_favorite_house (user_id, created_at);

CREATE TABLE appointment (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    house_id VARCHAR(36) NOT NULL,
    appointment_date DATE NOT NULL,
    time_slot VARCHAR(30) NOT NULL,
    contact_name VARCHAR(30) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    remark VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE rental_application (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    house_id VARCHAR(36) NOT NULL,
    lease_start_date DATE NOT NULL,
    lease_months INT NOT NULL,
    remark VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE conversation (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    house_id VARCHAR(36),
    landlord_id VARCHAR(36),
    source VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE conversation_message (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'text',
    content TEXT NOT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE message (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    icon_key VARCHAR(50),
    action_type VARCHAR(30) NOT NULL DEFAULT 'none',
    action_target VARCHAR(500),
    is_read TINYINT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_message_user_status ON message (user_id, is_deleted, is_read, created_at);

CREATE TABLE lease (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    house_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent INT NOT NULL,
    deposit INT NOT NULL,
    contract_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE lock_device (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    lock_name VARCHAR(100) NOT NULL,
    lock_brand VARCHAR(100),
    lock_sn VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'unknown',
    battery_level INT,
    gateway_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_lock_device_sn UNIQUE (lock_sn)
);

CREATE TABLE lock_permission (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    lease_id VARCHAR(36) NOT NULL,
    lock_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
