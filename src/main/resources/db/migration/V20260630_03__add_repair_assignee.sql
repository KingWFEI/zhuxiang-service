ALTER TABLE repair_record
    ADD COLUMN assignee VARCHAR(100) DEFAULT NULL COMMENT '当前处理人' AFTER repairman_name;
