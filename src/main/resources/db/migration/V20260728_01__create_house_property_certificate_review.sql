CREATE TABLE house_property_certificate (
    id VARCHAR(36) PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    landlord_id VARCHAR(36) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(700) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    audit_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    is_current TINYINT NOT NULL DEFAULT 1,
    review_remark VARCHAR(500),
    reviewer_id VARCHAR(36),
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_house_property_certificate_house (house_id, is_current, created_at),
    INDEX idx_house_property_certificate_landlord (landlord_id, created_at),
    INDEX idx_house_property_certificate_audit (audit_status, submitted_at)
) COMMENT = '房源房产证明材料及审核历史';

