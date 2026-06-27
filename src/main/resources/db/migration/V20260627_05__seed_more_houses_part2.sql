-- 追加 6 套种子房源

INSERT INTO house (
    id, title, cover_image, location, community_id, address, building, unit, room,
    price, deposit, payment_method, room_type, area, floor, orientation, decoration,
    available_date, metro, description, rent_type, status, is_smart_lock_supported,
    is_self_viewing_supported, landlord_id, view_count, favorite_count, created_at, updated_at
) VALUES
-- house-13: 推荐 · 阳光开间 经济实惠
(
    'house-13', '阳光开间 · 经济实惠', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg',
    '渝北区', 'community-1', '重庆市渝北区幸福小区 4 栋 1 单元 302',
    '4栋', '1单元', '302', 98000, 98000, '押一付一', '1室0厅1卫',
    25.00, '3/12层', '朝南', '简装', '2026-07-15', '距 3 号线 500m',
    '紧凑实用开间，租金实惠，适合独居。', 'recommended', 'available', 0, 0,
    'landlord-1', 30, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-14: 推荐 · 轻奢三居 品质之选
(
    'house-14', '轻奢三居 · 品质之选', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1',
    '江北区', 'community-2', '重庆市江北区星河公寓 12 栋 1 单元 1801',
    '12栋', '1单元', '1801', 658000, 658000, '押一付三', '3室2厅2卫',
    110.00, '18/32层', '南北通透', '豪装', '2026-07-01', '距 9 号线 150m',
    '轻奢装修，品牌家电齐全，视野开阔。', 'recommended', 'available', 1, 1,
    'landlord-2', 78, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-15: 短租 · 温馨小筑 月租优惠
(
    'house-15', '温馨小筑 · 月租优惠', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg',
    '渝北区', 'community-1', '重庆市渝北区幸福小区 2 栋 3 单元 601',
    '2栋', '3单元', '601', 128000, 128000, '押一付一', '1室0厅1卫',
    20.00, '6/8层', '朝东', '简装', '2026-07-10', '距 3 号线 650m',
    '温馨小户型，短租灵活，月租优惠。', 'short_rent', 'available', 1, 0,
    'landlord-1', 55, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-16: 民宿 · 田园小筑 清新风格
(
    'house-16', '田园小筑 · 清新风格', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg',
    '渝中区', 'community-3', '重庆市渝中区江景雅苑 5 栋 2 单元 1203',
    '5栋', '2单元', '1203', 31800, 80000, '押一付一', '1室0厅1卫',
    28.00, '12/30层', '朝南', '田园风', '2026-07-05', '距 2 号线 450m',
    '田园风格装修，温馨治愈，适合周末度假。', 'homestay', 'available', 1, 0,
    'landlord-2', 42, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-17: 长租 · 舒适一居 安静宜居
(
    'house-17', '舒适一居 · 安静宜居', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg',
    '江北区', 'community-2', '重庆市江北区星河公寓 3 栋 1 单元 905',
    '3栋', '1单元', '905', 188000, 188000, '押一付一', '1室1厅1卫',
    45.00, '9/22层', '朝南', '精装修', '2026-07-20', '距 9 号线 400m',
    '环境安静，通勤便利，适合上班族长租。', 'long_rent', 'available', 1, 1,
    'landlord-1', 63, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-18: 长租 · 品质三居 家庭首选
(
    'house-18', '品质三居 · 家庭首选', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1',
    '渝中区', 'community-3', '重庆市渝中区江景雅苑 8 栋 1 单元 2202',
    '8栋', '1单元', '2202', 398000, 398000, '押一付三', '3室2厅1卫',
    95.00, '22/30层', '南北通透', '精装修', '2026-07-01', '距 2 号线 300m',
    '三居室大空间，配套成熟，适合家庭居住。', 'long_rent', 'available', 1, 1,
    'landlord-2', 88, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- 标签关联
INSERT INTO house_tag_relation (id, house_id, tag_id) VALUES
('htr-24', 'house-13', 'tag-metro'),
('htr-25', 'house-13', 'tag-monthly'),
('htr-26', 'house-14', 'tag-metro'),
('htr-27', 'house-14', 'tag-smart-lock'),
('htr-28', 'house-15', 'tag-ready'),
('htr-29', 'house-15', 'tag-smart-lock'),
('htr-30', 'house-16', 'tag-smart-lock'),
('htr-31', 'house-17', 'tag-metro'),
('htr-32', 'house-17', 'tag-smart-lock'),
('htr-33', 'house-17', 'tag-monthly'),
('htr-34', 'house-18', 'tag-metro'),
('htr-35', 'house-18', 'tag-smart-lock');

-- 设施关联
INSERT INTO house_facility_relation (id, house_id, facility_id) VALUES
('hfr-34', 'house-13', 'air_conditioner'),
('hfr-35', 'house-13', 'wifi'),
('hfr-36', 'house-14', 'air_conditioner'),
('hfr-37', 'house-14', 'refrigerator'),
('hfr-38', 'house-14', 'washing_machine'),
('hfr-39', 'house-14', 'wifi'),
('hfr-40', 'house-15', 'air_conditioner'),
('hfr-41', 'house-15', 'wifi'),
('hfr-42', 'house-16', 'air_conditioner'),
('hfr-43', 'house-16', 'wifi'),
('hfr-44', 'house-17', 'air_conditioner'),
('hfr-45', 'house-17', 'wifi'),
('hfr-46', 'house-17', 'washing_machine'),
('hfr-47', 'house-18', 'air_conditioner'),
('hfr-48', 'house-18', 'refrigerator'),
('hfr-49', 'house-18', 'washing_machine'),
('hfr-50', 'house-18', 'wifi');
