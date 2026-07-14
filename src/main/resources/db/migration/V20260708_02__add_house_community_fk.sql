-- 1. community_id 改为可空（旧数据可能无对应小区）
ALTER TABLE house MODIFY COLUMN community_id VARCHAR(36) NULL COMMENT '小区ID';

-- 2. 清理无效引用（置 NULL）
UPDATE house
SET community_id = NULL
WHERE community_id IS NOT NULL
  AND community_id NOT IN (SELECT id FROM community);

-- 3. 添加外键约束
ALTER TABLE house
    ADD CONSTRAINT fk_house_community FOREIGN KEY (community_id) REFERENCES community(id);
