-- Integration-test-owned catalog. Production migrations intentionally remove these demo IDs.

INSERT IGNORE INTO `user` (
    id, phone, password_hash, nickname, avatar_url, role, status, created_at, updated_at
) VALUES
    ('landlord-1', '13900139000', NULL, '测试房东一', '', 'LANDLORD', 'inactive', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('landlord-2', '13700137000', NULL, '测试房东二', '', 'LANDLORD', 'inactive', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO landlord (
    id, user_id, name, avatar_url, phone, is_verified, rating, rented_count,
    response_description, created_at, updated_at
) VALUES
    ('landlord-1', 'landlord-1', '张先生', '', '13900139000', 1, 4.90, 23, '回复及时', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('landlord-2', 'landlord-2', '李女士', '', '13700137000', 1, 4.80, 18, '服务专业', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO region (
    id, parent_id, name, code, level, sort_order, enabled, created_at, updated_at
) VALUES
    ('region-yubei', NULL, '渝北区', 'yubei', 'district', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('region-jiangbei', NULL, '江北区', 'jiangbei', 'district', 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('region-yuzhong', NULL, '渝中区', 'yuzhong', 'district', 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO community (
    id, region_id, name, address, latitude, longitude, created_at, updated_at,
    normalized_name, province, city, district, ad_code, coordinate_system, map_provider, status
) VALUES
    ('community-1', 'region-yubei', '幸福小区', '重庆市渝北区幸福路88号', 29.6500000, 106.5500000,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '幸福小区', '重庆市', '重庆市', '渝北区', '500112', 'GCJ02', 'amap', 'approved'),
    ('community-2', 'region-jiangbei', '星河公寓', '重庆市江北区观音桥街道', 29.5800000, 106.5300000,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '星河公寓', '重庆市', '重庆市', '江北区', '500105', 'GCJ02', 'amap', 'approved'),
    ('community-3', 'region-yuzhong', '江景雅苑', '重庆市渝中区滨江路', 29.5600000, 106.5700000,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '江景雅苑', '重庆市', '重庆市', '渝中区', '500103', 'GCJ02', 'amap', 'approved');

INSERT IGNORE INTO house (
    id, title, cover_image, location, community_id, address, building, unit, room,
    price, deposit, payment_method, room_type, area, floor, orientation, decoration,
    available_date, metro, description, rent_mode, rent_type, status,
    is_smart_lock_supported, is_self_viewing_supported, landlord_id, source_type,
    view_count, favorite_count, created_at, updated_at
) VALUES
    ('house-1', '温馨一居 · 阳光充足', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1',
     '渝北区', 'community-1', '重庆市渝北区幸福小区3栋2单元1201', '3栋', '2单元', '1201',
     268000, 268000, '押一付一', '1室1厅1卫', 42.00, '12/28层', '朝南', '精装修',
     CURRENT_DATE, '距3号线500m', '采光好，交通便利，拎包入住。', 'WHOLE_RENT', 'LONG_RENT', 'available',
     1, 1, '00000000-0000-0000-0000-000000000001', 'PLATFORM', 120, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('house-2', '轻奢两居 · 近地铁', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg',
     '江北区', 'community-2', '重庆市江北区星河公寓2栋1单元801', '2栋', '1单元', '801',
     598000, 598000, '押一付三', '2室1厅1卫', 78.00, '8/18层', '南北通透', '精装修',
     CURRENT_DATE, '距9号线300m', '宽敞两居，靠近地铁和商业配套。', 'WHOLE_RENT', 'LONG_RENT', 'available',
     1, 0, '00000000-0000-0000-0000-000000000001', 'PLATFORM', 80, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('house-3', '江景民宿 · 一线夜景', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg',
     '渝中区', 'community-3', '重庆市渝中区江景雅苑1栋1802', '1栋', '1单元', '1802',
     38800, 38800, '押一付一', '1室1厅1卫', 50.00, '18/30层', '朝东', '品质装修',
     CURRENT_DATE, '距2号线600m', '临江高层民宿，适合短途旅行。', 'WHOLE_RENT', 'HOMESTAY', 'available',
     1, 1, '00000000-0000-0000-0000-000000000001', 'PLATFORM', 66, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('house-4', '短租单间 · 拎包入住', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg',
     '渝北区', 'community-1', '重庆市渝北区幸福小区5栋603', '5栋', '1单元', '603',
     168000, 168000, '押一付一', '1室0厅1卫', 28.00, '6/12层', '朝南', '简装',
     CURRENT_DATE, '距3号线700m', '紧凑舒适单间，可按月短租。', 'WHOLE_RENT', 'SHORT_RENT', 'available',
     0, 0, '00000000-0000-0000-0000-000000000001', 'PLATFORM', 35, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO house_location (
    id, house_id, longitude, latitude, province, city, district, created_at, updated_at
) VALUES
    ('location-house-1', 'house-1', 106.5500000, 29.6500000, '重庆市', '重庆市', '渝北区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('location-house-2', 'house-2', 106.5300000, 29.5800000, '重庆市', '重庆市', '江北区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('location-house-3', 'house-3', 106.5700000, 29.5600000, '重庆市', '重庆市', '渝中区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('location-house-4', 'house-4', 106.5520000, 29.6520000, '重庆市', '重庆市', '渝北区', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO house_image (id, house_id, image_url, image_type, title, sort_order, created_at) VALUES
    ('image-1-1', 'house-1', 'https://imgs.699pic.com/images/601/077/554.jpg!detail.v1', 'cover', '客厅', 1, CURRENT_TIMESTAMP),
    ('image-1-2', 'house-1', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg', 'bedroom', '卧室', 2, CURRENT_TIMESTAMP),
    ('image-2-1', 'house-2', 'https://img95.699pic.com/photo/60003/2333.jpg_wh860.jpg', 'cover', '客厅', 1, CURRENT_TIMESTAMP),
    ('image-3-1', 'house-3', 'https://img95.699pic.com/photo/50127/8985.jpg_wh860.jpg', 'cover', '江景', 1, CURRENT_TIMESTAMP),
    ('image-4-1', 'house-4', 'https://img95.699pic.com/photo/50283/7433.jpg_wh860.jpg', 'cover', '卧室', 1, CURRENT_TIMESTAMP);

INSERT IGNORE INTO house_tag_relation (id, house_id, tag_id) VALUES
    ('htr-1', 'house-1', 'tag-metro'), ('htr-2', 'house-1', 'tag-monthly'),
    ('htr-3', 'house-1', 'tag-smart-lock'), ('htr-4', 'house-2', 'tag-metro'),
    ('htr-5', 'house-2', 'tag-smart-lock'), ('htr-6', 'house-3', 'tag-smart-lock'),
    ('htr-7', 'house-4', 'tag-ready');

INSERT IGNORE INTO house_facility_relation (id, house_id, facility_id) VALUES
    ('hfr-1', 'house-1', 'air_conditioner'), ('hfr-2', 'house-1', 'washing_machine'),
    ('hfr-3', 'house-1', 'refrigerator'), ('hfr-4', 'house-1', 'wifi'),
    ('hfr-5', 'house-2', 'air_conditioner'), ('hfr-6', 'house-2', 'washing_machine'),
    ('hfr-7', 'house-2', 'wifi'), ('hfr-8', 'house-3', 'air_conditioner'),
    ('hfr-9', 'house-3', 'wifi'), ('hfr-10', 'house-4', 'air_conditioner'),
    ('hfr-11', 'house-4', 'wifi');

INSERT IGNORE INTO advertisement (
    id, title, description, image_url, target_type, target_value, position,
    enabled, sort_order, created_at, updated_at
) VALUES (
    'ad-1', '品质好房', '精选品牌公寓', '', 'house', 'house-1', 'home_feed',
    1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
