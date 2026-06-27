-- 追加 8 套房源数据，复用已有的图片、小区、标签和设施

INSERT INTO house (
    id, title, cover_image, location, community_id, address, building, unit, room,
    price, deposit, payment_method, room_type, area, floor, orientation, decoration,
    available_date, metro, description, rent_type, status, is_smart_lock_supported,
    is_self_viewing_supported, landlord_id, view_count, favorite_count, created_at, updated_at
) VALUES
-- house-5: 推荐 · 精装两居 近商圈
(
    'house-5', '精装两居 · 近商圈', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg',
    '渝北区', 'community-1', '重庆市渝北区幸福小区 7 栋 1 单元 1503',
    '7栋', '1单元', '1503', 328000, 328000, '押一付一', '2室1厅1卫',
    68.00, '15/25层', '朝南', '精装修', '2026-07-05', '距 3 号线 400m',
    '紧邻商圈，生活便利，品牌家电齐全。', 'recommended', 'available', 1, 1,
    'landlord-1', 95, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-6: 推荐 · 豪华套间 大阳台
(
    'house-6', '豪华套间 · 大阳台', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1',
    '江北区', 'community-2', '重庆市江北区星河公寓 5 栋 2 单元 2206',
    '5栋', '2单元', '2206', 458000, 458000, '押一付三', '1室1厅1卫',
    55.00, '22/28层', '南北通透', '豪华装修', '2026-07-10', '距 9 号线 200m',
    '高层视野开阔，带超大景观阳台。', 'recommended', 'available', 1, 1,
    'landlord-2', 110, 9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-7: 长租 · 经济两居 高性价比
(
    'house-7', '经济两居 · 高性价比', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg',
    '渝北区', 'community-1', '重庆市渝北区幸福小区 9 栋 3 单元 502',
    '9栋', '3单元', '502', 228000, 228000, '押一付一', '2室1厅1卫',
    72.00, '5/18层', '朝南', '简装', '2026-07-01', '距 3 号线 800m',
    '户型方正，性价比高，适合长租。', 'long_rent', 'available', 0, 0,
    'landlord-1', 45, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-8: 长租 · 花园洋房 三居室
(
    'house-8', '花园洋房 · 三居室', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg',
    '江北区', 'community-2', '重庆市江北区星河公寓 8 栋 1 单元 402',
    '8栋', '1单元', '402', 728000, 728000, '押一付三', '3室2厅2卫',
    120.00, '4/9层', '南北通透', '精装修', '2026-06-28', '距 环线 500m',
    '低密度花园洋房，安静舒适，适合家庭居住。', 'long_rent', 'available', 1, 0,
    'landlord-2', 55, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-9: 民宿 · 精致 loft
(
    'house-9', '精致 Loft · 网红打卡', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg',
    '渝中区', 'community-3', '重庆市渝中区江景雅苑 3 栋 2503',
    '3栋', '1单元', '2503', 42800, 100000, '押一付一', '1室1厅1卫',
    35.00, '25/30层', '朝东', '品质装修', '2026-06-26', '距 2 号线 500m',
    '网红 Loft 设计，夜景绝佳，适合情侣出行。', 'homestay', 'available', 1, 1,
    'landlord-1', 88, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-10: 民宿 · 清新治愈风
(
    'house-10', '清新治愈 · 原木风', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg',
    '渝中区', 'community-3', '重庆市渝中区江景雅苑 2 栋 1608',
    '2栋', '2单元', '1608', 35800, 80000, '押一付一', '1室0厅1卫',
    30.00, '16/30层', '朝南', '品质装修', '2026-06-27', '距 2 号线 650m',
    '原木治愈风装修，温馨舒适。', 'homestay', 'available', 1, 1,
    'landlord-2', 72, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-11: 短租 · 阳光单间
(
    'house-11', '阳光单间 · 月租优惠', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg',
    '渝北区', 'community-1', '重庆市渝北区幸福小区 2 栋 2 单元 705',
    '2栋', '2单元', '705', 138000, 138000, '押一付一', '1室0厅1卫',
    22.00, '7/12层', '朝南', '简装', '2026-07-01', '距 3 号线 600m',
    '阳光充足，月租优惠，适合短租过渡。', 'short_rent', 'available', 0, 0,
    'landlord-1', 28, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
-- house-12: 短租 · 商务公寓
(
    'house-12', '商务公寓 · 拎包入住', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1',
    '江北区', 'community-2', '重庆市江北区星河公寓 6 栋 1 单元 1101',
    '6栋', '1单元', '1101', 198000, 198000, '押一付一', '1室1厅1卫',
    40.00, '11/20层', '朝北', '精装修', '2026-06-30', '距 9 号线 350m',
    '商务风格装修，配套齐全，差旅短租首选。', 'short_rent', 'available', 1, 1,
    'landlord-2', 38, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- 插入房源图片
INSERT INTO house_image (id, house_id, image_url, image_type, title, sort_order, created_at) VALUES
-- house-5 图片
('image-5-1', 'house-5', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg', 'cover', '客厅', 1, CURRENT_TIMESTAMP),
('image-5-2', 'house-5', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1', 'bedroom', '主卧', 2, CURRENT_TIMESTAMP),
('image-5-3', 'house-5', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg', 'kitchen', '厨房', 3, CURRENT_TIMESTAMP),
-- house-6 图片
('image-6-1', 'house-6', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1', 'cover', '客厅', 1, CURRENT_TIMESTAMP),
('image-6-2', 'house-6', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg', 'bedroom', '卧室', 2, CURRENT_TIMESTAMP),
('image-6-3', 'house-6', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg', 'balcony', '阳台', 3, CURRENT_TIMESTAMP),
-- house-7 图片
('image-7-1', 'house-7', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg', 'cover', '客厅', 1, CURRENT_TIMESTAMP),
('image-7-2', 'house-7', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg', 'bedroom', '卧室', 2, CURRENT_TIMESTAMP),
-- house-8 图片
('image-8-1', 'house-8', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg', 'cover', '客厅', 1, CURRENT_TIMESTAMP),
('image-8-2', 'house-8', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg', 'bedroom', '主卧', 2, CURRENT_TIMESTAMP),
('image-8-3', 'house-8', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1', 'bedroom', '次卧', 3, CURRENT_TIMESTAMP),
('image-8-4', 'house-8', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg', 'kitchen', '厨房', 4, CURRENT_TIMESTAMP),
-- house-9 图片
('image-9-1', 'house-9', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg', 'cover', '客厅', 1, CURRENT_TIMESTAMP),
('image-9-2', 'house-9', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg', 'bedroom', '卧室', 2, CURRENT_TIMESTAMP),
-- house-10 图片
('image-10-1', 'house-10', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg', 'cover', '卧室', 1, CURRENT_TIMESTAMP),
('image-10-2', 'house-10', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg', 'living_room', '客厅', 2, CURRENT_TIMESTAMP),
-- house-11 图片
('image-11-1', 'house-11', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg', 'cover', '卧室', 1, CURRENT_TIMESTAMP),
-- house-12 图片
('image-12-1', 'house-12', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1', 'cover', '客厅', 1, CURRENT_TIMESTAMP),
('image-12-2', 'house-12', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg', 'bedroom', '卧室', 2, CURRENT_TIMESTAMP),
('image-12-3', 'house-12', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg', 'bathroom', '卫生间', 3, CURRENT_TIMESTAMP);

-- 关联标签
INSERT INTO house_tag_relation (id, house_id, tag_id) VALUES
('htr-8',  'house-5',  'tag-metro'),
('htr-9',  'house-5',  'tag-monthly'),
('htr-10', 'house-5',  'tag-ready'),
('htr-11', 'house-6',  'tag-metro'),
('htr-12', 'house-6',  'tag-smart-lock'),
('htr-13', 'house-7',  'tag-monthly'),
('htr-14', 'house-7',  'tag-ready'),
('htr-15', 'house-8',  'tag-metro'),
('htr-16', 'house-8',  'tag-smart-lock'),
('htr-17', 'house-9',  'tag-smart-lock'),
('htr-18', 'house-10', 'tag-smart-lock'),
('htr-19', 'house-10', 'tag-ready'),
('htr-20', 'house-11', 'tag-ready'),
('htr-21', 'house-12', 'tag-metro'),
('htr-22', 'house-12', 'tag-smart-lock'),
('htr-23', 'house-12', 'tag-ready');

-- 关联设施
INSERT INTO house_facility_relation (id, house_id, facility_id) VALUES
('hfr-12', 'house-5',  'air_conditioner'),
('hfr-13', 'house-5',  'washing_machine'),
('hfr-14', 'house-5',  'refrigerator'),
('hfr-15', 'house-5',  'wifi'),
('hfr-16', 'house-6',  'air_conditioner'),
('hfr-17', 'house-6',  'washing_machine'),
('hfr-18', 'house-6',  'wifi'),
('hfr-19', 'house-7',  'air_conditioner'),
('hfr-20', 'house-7',  'wifi'),
('hfr-21', 'house-8',  'air_conditioner'),
('hfr-22', 'house-8',  'washing_machine'),
('hfr-23', 'house-8',  'refrigerator'),
('hfr-24', 'house-8',  'wifi'),
('hfr-25', 'house-9',  'air_conditioner'),
('hfr-26', 'house-9',  'wifi'),
('hfr-27', 'house-10', 'air_conditioner'),
('hfr-28', 'house-10', 'wifi'),
('hfr-29', 'house-11', 'air_conditioner'),
('hfr-30', 'house-11', 'wifi'),
('hfr-31', 'house-12', 'air_conditioner'),
('hfr-32', 'house-12', 'washing_machine'),
('hfr-33', 'house-12', 'wifi');

-- 为部分房源添加智能门锁
INSERT INTO lock_device (id, house_id, lock_name, lock_brand, lock_sn, status, battery_level, gateway_id, created_at, updated_at) VALUES
('lock-2', 'house-5', '1503门锁', '住享智能锁', 'ZX-LOCK-0002', 'online', 92, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lock-3', 'house-6', '2206门锁', '住享智能锁', 'ZX-LOCK-0003', 'online', 85, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lock-4', 'house-8', '402门锁', '住享智能锁', 'ZX-LOCK-0004', 'online', 78, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lock-5', 'house-9', '2503门锁', '住享智能锁', 'ZX-LOCK-0005', 'online', 90, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lock-6', 'house-10', '1608门锁', '住享智能锁', 'ZX-LOCK-0006', 'online', 95, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lock-7', 'house-12', '1101门锁', '住享智能锁', 'ZX-LOCK-0007', 'online', 82, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
