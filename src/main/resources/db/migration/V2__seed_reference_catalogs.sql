-- 仅初始化房源创建必需的标准户型字典，不创建任何样例业务记录。

INSERT INTO house_room_type (id, name, sort_order, enabled, created_at, updated_at) VALUES
    ('room-type-1-0-1', '1室0厅1卫', 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-1-1-1', '1室1厅1卫', 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-2-1-1', '2室1厅1卫', 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-2-2-1', '2室2厅1卫', 40, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-3-1-1', '3室1厅1卫', 50, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-3-2-2', '3室2厅2卫', 60, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('room-type-4-2-2', '4室2厅2卫', 70, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
