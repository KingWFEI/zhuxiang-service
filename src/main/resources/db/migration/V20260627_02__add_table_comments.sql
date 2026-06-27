-- 为所有表和字段添加中文注释

-- ==================== 用户模块 ====================
ALTER TABLE `user` COMMENT = '用户表';
ALTER TABLE `user`
    MODIFY COLUMN phone VARCHAR(20) NOT NULL COMMENT '手机号（登录账号）',
    MODIFY COLUMN password_hash VARCHAR(100) COMMENT '密码哈希',
    MODIFY COLUMN nickname VARCHAR(30) NOT NULL COMMENT '用户昵称',
    MODIFY COLUMN avatar_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '头像URL',
    MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'TENANT' COMMENT '角色：TENANT租客/LANDLORD房东/ADMIN管理员',
    MODIFY COLUMN is_verified TINYINT NOT NULL DEFAULT 0 COMMENT '是否实名认证：0否，1是',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active正常/disabled禁用/cancelled注销',
    MODIFY COLUMN last_login_at TIMESTAMP NULL COMMENT '最后登录时间';

ALTER TABLE sms_code COMMENT = '短信验证码表';
ALTER TABLE sms_code
    MODIFY COLUMN phone VARCHAR(20) NOT NULL COMMENT '手机号',
    MODIFY COLUMN scene VARCHAR(30) NOT NULL COMMENT '场景：login登录/register注册等',
    MODIFY COLUMN code VARCHAR(10) NOT NULL COMMENT '验证码',
    MODIFY COLUMN expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    MODIFY COLUMN used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用：0否，1是',
    MODIFY COLUMN used_at TIMESTAMP NULL COMMENT '使用时间';

ALTER TABLE refresh_token COMMENT = '刷新令牌表';
ALTER TABLE refresh_token
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN refresh_token VARCHAR(128) NOT NULL COMMENT '刷新令牌',
    MODIFY COLUMN expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    MODIFY COLUMN revoked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已撤销：0否，1是',
    MODIFY COLUMN revoked_at TIMESTAMP NULL COMMENT '撤销时间';

-- ==================== 公共基础 ====================
ALTER TABLE region COMMENT = '行政区域表';
ALTER TABLE region
    MODIFY COLUMN parent_id VARCHAR(36) COMMENT '上级区域ID',
    MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '区域名称',
    MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT '区域编码',
    MODIFY COLUMN level VARCHAR(30) NOT NULL COMMENT '层级：city城市/district区县',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是';

ALTER TABLE community COMMENT = '小区表';
ALTER TABLE community
    MODIFY COLUMN region_id VARCHAR(36) NOT NULL COMMENT '所属区域ID',
    MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '小区名称',
    MODIFY COLUMN address VARCHAR(500) COMMENT '详细地址',
    MODIFY COLUMN latitude DECIMAL(10, 7) COMMENT '纬度坐标',
    MODIFY COLUMN longitude DECIMAL(10, 7) COMMENT '经度坐标';

ALTER TABLE landlord COMMENT = '房东/管家表';
ALTER TABLE landlord
    MODIFY COLUMN name VARCHAR(50) NOT NULL COMMENT '姓名',
    MODIFY COLUMN avatar_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '头像URL',
    MODIFY COLUMN phone VARCHAR(20) COMMENT '联系电话',
    MODIFY COLUMN is_verified TINYINT NOT NULL DEFAULT 0 COMMENT '是否认证：0否，1是',
    MODIFY COLUMN rating DECIMAL(3, 2) NOT NULL DEFAULT 0 COMMENT '评分',
    MODIFY COLUMN rented_count INT NOT NULL DEFAULT 0 COMMENT '已租出数量',
    MODIFY COLUMN response_description VARCHAR(100) COMMENT '响应描述';

-- ==================== 房源 ====================
ALTER TABLE house COMMENT = '房源主表';
ALTER TABLE house
    MODIFY COLUMN title VARCHAR(200) NOT NULL COMMENT '房源标题',
    MODIFY COLUMN cover_image VARCHAR(500) NOT NULL DEFAULT '' COMMENT '封面图URL',
    MODIFY COLUMN location VARCHAR(100) NOT NULL COMMENT '位置描述（如：渝北区）',
    MODIFY COLUMN community_id VARCHAR(36) NOT NULL COMMENT '所属小区ID',
    MODIFY COLUMN address VARCHAR(500) COMMENT '详细地址',
    MODIFY COLUMN building VARCHAR(30) COMMENT '楼栋号',
    MODIFY COLUMN unit VARCHAR(30) COMMENT '单元号',
    MODIFY COLUMN room VARCHAR(30) COMMENT '房间号',
    MODIFY COLUMN price INT NOT NULL COMMENT '月租金（单位：分）',
    MODIFY COLUMN deposit INT NOT NULL DEFAULT 0 COMMENT '押金（单位：分）',
    MODIFY COLUMN payment_method VARCHAR(50) COMMENT '付款方式（如：押一付一）',
    MODIFY COLUMN room_type VARCHAR(50) COMMENT '户型（如：1室1厅1卫）',
    MODIFY COLUMN area DECIMAL(8, 2) COMMENT '面积（平方米）',
    MODIFY COLUMN floor VARCHAR(50) COMMENT '楼层描述（如：12/28层）',
    MODIFY COLUMN orientation VARCHAR(50) COMMENT '朝向（如：朝南）',
    MODIFY COLUMN decoration VARCHAR(50) COMMENT '装修情况（如：精装修）',
    MODIFY COLUMN available_date DATE COMMENT '可入住日期',
    MODIFY COLUMN metro VARCHAR(100) COMMENT '地铁交通信息（如：距3号线500m）',
    MODIFY COLUMN description TEXT COMMENT '房源描述',
    MODIFY COLUMN rent_type VARCHAR(30) NOT NULL COMMENT '租赁分类：recommended推荐/short_rent短租/homestay民宿/long_rent长租',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'available' COMMENT '房源状态：draft草稿/available可租/reserved已被预定/rented已租/offline下架',
    MODIFY COLUMN is_smart_lock_supported TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持智能门锁：0否，1是',
    MODIFY COLUMN is_self_viewing_supported TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持自主看房：0否，1是',
    MODIFY COLUMN landlord_id VARCHAR(36) NOT NULL COMMENT '关联房东/管家ID',
    MODIFY COLUMN view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    MODIFY COLUMN favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
    MODIFY COLUMN smart_lock_id VARCHAR(36) COMMENT '当前绑定的智能门锁ID',
    MODIFY COLUMN lock_bind_status VARCHAR(32) NOT NULL DEFAULT 'UNBOUND' COMMENT '门锁绑定状态：UNBOUND未绑定/BOUND已绑定';

ALTER TABLE house_image COMMENT = '房源图片表';
ALTER TABLE house_image
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    MODIFY COLUMN image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
    MODIFY COLUMN image_type VARCHAR(30) NOT NULL DEFAULT 'normal' COMMENT '图片类型：cover封面/bedroom卧室/living_room客厅/kitchen厨房/bathroom卫生间/balcony阳台/normal普通',
    MODIFY COLUMN title VARCHAR(100) COMMENT '图片标题',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号';

ALTER TABLE house_tag COMMENT = '房源标签表';
ALTER TABLE house_tag
    MODIFY COLUMN name VARCHAR(50) NOT NULL COMMENT '标签名称',
    MODIFY COLUMN tag_type VARCHAR(30) COMMENT '标签类型：traffic交通/payment付款/feature特色',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是';

ALTER TABLE house_tag_relation COMMENT = '房源标签关联表';
ALTER TABLE house_tag_relation
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    MODIFY COLUMN tag_id VARCHAR(36) NOT NULL COMMENT '标签ID';

ALTER TABLE house_facility COMMENT = '房源设施表';
ALTER TABLE house_facility
    MODIFY COLUMN name VARCHAR(50) NOT NULL COMMENT '设施名称',
    MODIFY COLUMN icon_key VARCHAR(50) COMMENT '图标标识',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是';

ALTER TABLE house_facility_relation COMMENT = '房源设施关联表';
ALTER TABLE house_facility_relation
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    MODIFY COLUMN facility_id VARCHAR(36) NOT NULL COMMENT '设施ID';

ALTER TABLE user_favorite_house COMMENT = '用户收藏房源表';
ALTER TABLE user_favorite_house
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID';

-- ==================== 广告 ====================
ALTER TABLE advertisement COMMENT = '广告表';
ALTER TABLE advertisement
    MODIFY COLUMN title VARCHAR(100) NOT NULL COMMENT '广告标题',
    MODIFY COLUMN description VARCHAR(500) COMMENT '广告描述',
    MODIFY COLUMN image_url VARCHAR(500) COMMENT '广告图片URL',
    MODIFY COLUMN target_type VARCHAR(30) NOT NULL COMMENT '跳转类型：house房源/url链接/route路由',
    MODIFY COLUMN target_value VARCHAR(500) COMMENT '跳转目标值',
    MODIFY COLUMN position VARCHAR(30) NOT NULL COMMENT '广告位标识',
    MODIFY COLUMN enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN start_time TIMESTAMP NULL COMMENT '生效开始时间',
    MODIFY COLUMN end_time TIMESTAMP NULL COMMENT '生效结束时间';

-- ==================== 预约看房 ====================
ALTER TABLE appointment COMMENT = '看房预约表';
ALTER TABLE appointment
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    MODIFY COLUMN appointment_date DATE NOT NULL COMMENT '预约日期',
    MODIFY COLUMN time_slot VARCHAR(30) NOT NULL COMMENT '预约时段（如：morning上午/afternoon下午）',
    MODIFY COLUMN contact_name VARCHAR(30) NOT NULL COMMENT '联系人姓名',
    MODIFY COLUMN contact_phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    MODIFY COLUMN remark VARCHAR(500) COMMENT '备注',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '预约状态：pending待确认/confirmed已确认/cancelled已取消/completed已完成/no_show爽约';

-- ==================== 会话消息 ====================
ALTER TABLE conversation COMMENT = '会话表';
ALTER TABLE conversation
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN house_id VARCHAR(36) COMMENT '关联房源ID',
    MODIFY COLUMN landlord_id VARCHAR(36) COMMENT '关联房东ID',
    MODIFY COLUMN source VARCHAR(30) NOT NULL COMMENT '来源',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active活跃/closed已关闭';

ALTER TABLE conversation_message COMMENT = '会话消息表';
ALTER TABLE conversation_message
    MODIFY COLUMN conversation_id VARCHAR(36) NOT NULL COMMENT '会话ID',
    MODIFY COLUMN sender_id VARCHAR(36) NOT NULL COMMENT '发送者ID',
    MODIFY COLUMN sender_type VARCHAR(20) NOT NULL COMMENT '发送者类型：user用户/landlord房东',
    MODIFY COLUMN content_type VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '内容类型：text文本/image图片',
    MODIFY COLUMN content TEXT NOT NULL COMMENT '消息内容',
    MODIFY COLUMN is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0否，1是';

ALTER TABLE message COMMENT = '通知消息表';
ALTER TABLE message
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '接收用户ID',
    MODIFY COLUMN category VARCHAR(30) NOT NULL COMMENT '消息分类',
    MODIFY COLUMN title VARCHAR(100) NOT NULL COMMENT '消息标题',
    MODIFY COLUMN content VARCHAR(1000) NOT NULL COMMENT '消息内容',
    MODIFY COLUMN icon_key VARCHAR(50) COMMENT '图标标识',
    MODIFY COLUMN action_type VARCHAR(30) NOT NULL DEFAULT 'none' COMMENT '操作类型：none无/routes路由跳转',
    MODIFY COLUMN action_target VARCHAR(500) COMMENT '操作目标',
    MODIFY COLUMN is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0否，1是',
    MODIFY COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否已删除：0否，1是',
    MODIFY COLUMN read_at TIMESTAMP NULL COMMENT '阅读时间';

-- ==================== 租约 ====================
ALTER TABLE lease COMMENT = '租约表';
ALTER TABLE lease
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '租客用户ID',
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    MODIFY COLUMN status VARCHAR(20) NOT NULL COMMENT '状态：active生效中/expired已到期/terminated已终止',
    MODIFY COLUMN start_date DATE NOT NULL COMMENT '租期开始日期',
    MODIFY COLUMN end_date DATE NOT NULL COMMENT '租期结束日期',
    MODIFY COLUMN lease_months INT NOT NULL DEFAULT 1 COMMENT '租期（月数）',
    MODIFY COLUMN payment_method VARCHAR(30) COMMENT '付款方式',
    MODIFY COLUMN payment_months INT NOT NULL DEFAULT 1 COMMENT '每次付几个月',
    MODIFY COLUMN monthly_rent INT NOT NULL COMMENT '月租金（单位：分）',
    MODIFY COLUMN deposit INT NOT NULL COMMENT '押金（单位：分）',
    MODIFY COLUMN service_fee INT NOT NULL DEFAULT 0 COMMENT '服务费（单位：分）',
    MODIFY COLUMN first_payment_amount INT NOT NULL DEFAULT 0 COMMENT '首期付款金额（单位：分）',
    MODIFY COLUMN contract_id VARCHAR(36) COMMENT '关联合同ID';

-- ==================== 租房订单 ====================
ALTER TABLE rent_order COMMENT = '租房订单表';
ALTER TABLE rent_order
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'pendingRealName' COMMENT '订单状态：pendingRealName待实名/pendingContract待确认合同/pendingPayment待支付/pendingSign待签约/completed已完成/cancelled已取消',
    MODIFY COLUMN start_date DATE NOT NULL COMMENT '租期开始日期',
    MODIFY COLUMN end_date DATE NOT NULL COMMENT '租期结束日期',
    MODIFY COLUMN lease_months INT NOT NULL COMMENT '租期（月数）',
    MODIFY COLUMN payment_method VARCHAR(30) COMMENT '付款方式',
    MODIFY COLUMN payment_months INT NOT NULL DEFAULT 1 COMMENT '每次付几个月',
    MODIFY COLUMN tenant_count INT NOT NULL DEFAULT 1 COMMENT '入住人数',
    MODIFY COLUMN tenant_name VARCHAR(30) COMMENT '租客姓名',
    MODIFY COLUMN tenant_phone VARCHAR(20) COMMENT '租客手机号',
    MODIFY COLUMN tenant_id_card VARCHAR(30) COMMENT '租客身份证号',
    MODIFY COLUMN monthly_rent INT NOT NULL COMMENT '月租金（单位：分）',
    MODIFY COLUMN deposit INT NOT NULL COMMENT '押金（单位：分）',
    MODIFY COLUMN service_fee INT NOT NULL DEFAULT 0 COMMENT '服务费（单位：分）',
    MODIFY COLUMN first_payment_amount INT NOT NULL COMMENT '首期付款金额（单位：分）',
    MODIFY COLUMN total_amount INT NOT NULL COMMENT '总金额（单位：分）',
    MODIFY COLUMN real_name_at TIMESTAMP NULL COMMENT '实名认证时间',
    MODIFY COLUMN contract_confirmed_at TIMESTAMP NULL COMMENT '合同确认时间',
    MODIFY COLUMN paid_at TIMESTAMP NULL COMMENT '支付时间',
    MODIFY COLUMN signed_at TIMESTAMP NULL COMMENT '签约时间',
    MODIFY COLUMN cancelled_at TIMESTAMP NULL COMMENT '取消时间',
    MODIFY COLUMN user_hidden TINYINT NOT NULL DEFAULT 0 COMMENT '用户是否隐藏：0否，1是',
    MODIFY COLUMN hidden_at TIMESTAMP NULL COMMENT '隐藏时间';

ALTER TABLE rent_contract COMMENT = '租房合同表';
ALTER TABLE rent_contract
    MODIFY COLUMN order_id VARCHAR(36) NOT NULL COMMENT '关联订单ID',
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '租客用户ID',
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    MODIFY COLUMN contract_no VARCHAR(64) NOT NULL COMMENT '合同编号',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '合同状态：draft草稿/signed已签署',
    MODIFY COLUMN tenant_name VARCHAR(30) NOT NULL COMMENT '租客姓名',
    MODIFY COLUMN tenant_phone VARCHAR(20) NOT NULL COMMENT '租客手机号',
    MODIFY COLUMN tenant_id_card VARCHAR(30) NOT NULL COMMENT '租客身份证号',
    MODIFY COLUMN start_date DATE NOT NULL COMMENT '租期开始日期',
    MODIFY COLUMN end_date DATE NOT NULL COMMENT '租期结束日期',
    MODIFY COLUMN lease_months INT NOT NULL COMMENT '租期（月数）',
    MODIFY COLUMN monthly_rent INT NOT NULL COMMENT '月租金（单位：分）',
    MODIFY COLUMN deposit INT NOT NULL COMMENT '押金（单位：分）',
    MODIFY COLUMN service_fee INT NOT NULL DEFAULT 0 COMMENT '服务费（单位：分）',
    MODIFY COLUMN payment_months INT NOT NULL DEFAULT 1 COMMENT '每次付几个月',
    MODIFY COLUMN first_payment_amount INT NOT NULL DEFAULT 0 COMMENT '首期付款金额（单位：分）',
    MODIFY COLUMN house_name VARCHAR(200) COMMENT '房源名称',
    MODIFY COLUMN room_name VARCHAR(100) COMMENT '房间名称',
    MODIFY COLUMN house_address VARCHAR(500) COMMENT '房源地址',
    MODIFY COLUMN id_card_front_url VARCHAR(512) COMMENT '身份证正面照URL',
    MODIFY COLUMN id_card_back_url VARCHAR(512) COMMENT '身份证背面照URL',
    MODIFY COLUMN signed_at TIMESTAMP NULL COMMENT '签署时间';

-- ==================== 账单 & 支付 ====================
ALTER TABLE rent_bill COMMENT = '租金账单表';
ALTER TABLE rent_bill
    MODIFY COLUMN lease_id VARCHAR(36) NOT NULL COMMENT '关联租约ID',
    MODIFY COLUMN paid_at TIMESTAMP NULL COMMENT '支付时间';

ALTER TABLE payment_record COMMENT = '支付记录表';
ALTER TABLE payment_record
    MODIFY COLUMN order_id VARCHAR(36) NOT NULL COMMENT '关联订单ID',
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '支付用户ID',
    MODIFY COLUMN paid_at TIMESTAMP NULL COMMENT '支付成功时间',
    MODIFY COLUMN callback_time TIMESTAMP NULL COMMENT '回调时间';

ALTER TABLE file_record COMMENT = '文件记录表';
ALTER TABLE file_record
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '上传用户ID',
    MODIFY COLUMN url VARCHAR(500) NOT NULL COMMENT '文件URL',
    MODIFY COLUMN biz_type VARCHAR(30) NOT NULL COMMENT '业务类型';

-- ==================== 门锁 ====================
ALTER TABLE lock_device COMMENT = '房源门锁设备表';
ALTER TABLE lock_device
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '所属房源ID',
    MODIFY COLUMN lock_name VARCHAR(100) NOT NULL COMMENT '门锁名称',
    MODIFY COLUMN lock_brand VARCHAR(100) COMMENT '门锁品牌',
    MODIFY COLUMN lock_sn VARCHAR(100) COMMENT '门锁序列号',
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'unknown' COMMENT '门锁状态：online在线/offline离线/unknown未知',
    MODIFY COLUMN battery_level INT COMMENT '电量百分比',
    MODIFY COLUMN gateway_id VARCHAR(36) COMMENT '网关ID';

ALTER TABLE lock_permission COMMENT = '门锁权限表';
ALTER TABLE lock_permission
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '用户ID',
    MODIFY COLUMN lease_id VARCHAR(36) NOT NULL COMMENT '关联租约ID',
    MODIFY COLUMN lock_id VARCHAR(36) NOT NULL COMMENT '门锁ID',
    MODIFY COLUMN status VARCHAR(20) NOT NULL COMMENT '权限状态：active生效/expired过期',
    MODIFY COLUMN valid_from TIMESTAMP NOT NULL COMMENT '有效期开始',
    MODIFY COLUMN valid_to TIMESTAMP NOT NULL COMMENT '有效期结束';

-- ==================== 已删除表标记 ====================
-- rental_application 已在 V20260626_04 中删除，无需添加注释
