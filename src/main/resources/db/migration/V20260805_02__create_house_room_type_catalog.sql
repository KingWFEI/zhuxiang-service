CREATE TABLE house_room_type (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_house_room_type_name UNIQUE (name)
) COMMENT='房源户型字典表';

INSERT IGNORE INTO house_room_type (id, name, sort_order, enabled, created_at, updated_at)
SELECT UUID(), existing.name, 100, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT DISTINCT TRIM(room_type) AS name
    FROM house
    WHERE room_type IS NOT NULL AND TRIM(room_type) <> ''
) existing;

INSERT IGNORE INTO house_room_type (id, name, sort_order, enabled, created_at, updated_at) VALUES
    ('room-type-1-0-1', '1室0厅1卫', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-1-1-1', '1室1厅1卫', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-2-1-1', '2室1厅1卫', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-2-2-1', '2室2厅1卫', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-3-1-1', '3室1厅1卫', 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-3-2-2', '3室2厅2卫', 60, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-4-2-2', '4室2厅2卫', 70, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
