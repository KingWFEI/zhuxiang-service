-- ============================================================
-- 沉浸式看房模块 - 数据库初始化
-- 合并自 immersive-tour-service V1+V2+V3+V4
-- ============================================================

-- ----------------------------
-- 1. immersive_tour 沉浸式项目
-- ----------------------------
CREATE TABLE IF NOT EXISTS immersive_tour (
    id              VARCHAR(32)     NOT NULL PRIMARY KEY COMMENT '项目ID',
    house_id        VARCHAR(32)     NOT NULL COMMENT '房源ID（引用原系统房源标识，无物理外键）',
    title           VARCHAR(200)    NOT NULL COMMENT '项目标题',
    cover_image_url VARCHAR(500)    NULL     COMMENT '封面图URL',
    floor_plan_url  VARCHAR(500)    NULL     COMMENT '户型图URL',
    entry_scene_id  VARCHAR(32)     NULL     COMMENT '入口房间ID',
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿 PUBLISHED-已发布 OFFLINE-已下线',
    published_at    DATETIME        NULL     COMMENT '发布时间',
    created_by      VARCHAR(36)     NOT NULL COMMENT '创建人ID',
    updated_by      VARCHAR(36)     NULL     COMMENT '更新人ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    active_key      VARCHAR(64)     NULL     COMMENT '唯一约束辅助字段：未删除时存house_id，删除时置NULL',
    deleted         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_imtour_house_id (house_id),
    INDEX idx_imtour_status (status),
    INDEX idx_imtour_deleted (deleted),
    UNIQUE INDEX uk_imtour_active_key (active_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='沉浸式看房项目';

-- ----------------------------
-- 2. immersive_scene 房间场景
-- ----------------------------
CREATE TABLE IF NOT EXISTS immersive_scene (
    id                  VARCHAR(32)     NOT NULL PRIMARY KEY COMMENT '场景ID',
    tour_id             VARCHAR(32)     NOT NULL COMMENT '所属项目ID',
    name                VARCHAR(100)    NOT NULL COMMENT '房间名称',
    scene_type          VARCHAR(30)     NOT NULL COMMENT '房间类型',
    entry_image_id      VARCHAR(32)     NULL     COMMENT '入口图片ID',
    floor_plan_x_ratio  DECIMAL(10,4)   NULL     COMMENT '户型图X坐标比例(0~1)',
    floor_plan_y_ratio  DECIMAL(10,4)   NULL     COMMENT '户型图Y坐标比例(0~1)',
    render_mode         VARCHAR(20)     NOT NULL DEFAULT 'PHOTO' COMMENT '渲染模式：PHOTO-普通图片 PANORAMA-全景',
    initial_yaw         DECIMAL(10,4)   NULL     COMMENT '全景初始水平角度(-180~180)',
    initial_pitch       DECIMAL(10,4)   NULL     COMMENT '全景初始垂直角度(-90~90)',
    initial_hfov        DECIMAL(10,4)   NULL     COMMENT '全景初始视场角(>0)',
    sort_order          INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    enabled             TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_imscene_tour_id (tour_id),
    INDEX idx_imscene_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间场景';

-- ----------------------------
-- 3. immersive_scene_image 场景图片
-- ----------------------------
CREATE TABLE IF NOT EXISTS immersive_scene_image (
    id              VARCHAR(32)     NOT NULL PRIMARY KEY COMMENT '图片ID',
    scene_id        VARCHAR(32)     NOT NULL COMMENT '所属场景ID',
    name            VARCHAR(100)    NULL     COMMENT '图片名称/站位名称',
    image_url       VARCHAR(500)    NOT NULL COMMENT '图片URL',
    thumbnail_url   VARCHAR(500)    NULL     COMMENT '缩略图URL',
    width           INT             NULL     COMMENT '图片宽度(像素)',
    height          INT             NULL     COMMENT '图片高度(像素)',
    projection_type VARCHAR(20)     NOT NULL DEFAULT 'FLAT' COMMENT '投影类型：FLAT-平面 EQUIRECTANGULAR-等距柱状',
    image_width     INT             NULL     COMMENT '全景图片原始宽度(像素)',
    image_height    INT             NULL     COMMENT '全景图片原始高度(像素)',
    sort_order      INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    is_entry        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否为入口图片：0-否 1-是',
    enabled         TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_imimg_scene_id (scene_id),
    INDEX idx_imimg_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景图片';

-- ----------------------------
-- 4. immersive_image_hotspot 图片热点
-- ----------------------------
CREATE TABLE IF NOT EXISTS immersive_image_hotspot (
    id              VARCHAR(32)     NOT NULL PRIMARY KEY COMMENT '热点ID',
    source_image_id VARCHAR(32)     NOT NULL COMMENT '源图片ID（必须是入口图片）',
    label           VARCHAR(100)    NOT NULL COMMENT '热点标签',
    x_ratio         DECIMAL(10,4)   NULL     COMMENT 'X坐标比例(0~1)，PHOTO模式必填',
    y_ratio         DECIMAL(10,4)   NULL     COMMENT 'Y坐标比例(0~1)，PHOTO模式必填',
    yaw             DECIMAL(10,4)   NULL     COMMENT '全景水平角度(-180~180)',
    pitch           DECIMAL(10,4)   NULL     COMMENT '全景垂直角度(-90~90)',
    target_type     VARCHAR(10)     NOT NULL DEFAULT 'SCENE' COMMENT '跳转类型：SCENE-跳转场景入口图 IMAGE-跳转指定图片',
    target_scene_id VARCHAR(32)     NOT NULL COMMENT '目标房间ID',
    target_image_id VARCHAR(32)     NULL     COMMENT '目标图片ID（target_type=IMAGE 时必填）',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    INDEX idx_imhs_source_image_id (source_image_id),
    INDEX idx_imhs_target_scene_id (target_scene_id),
    INDEX idx_imhs_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片热点';
