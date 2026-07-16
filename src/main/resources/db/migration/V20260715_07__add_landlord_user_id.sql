-- 房东表增加关联系统用户ID，用于房东登录签署合同
ALTER TABLE landlord
    ADD COLUMN user_id VARCHAR(36) NULL COMMENT '关联系统用户ID，用于房东以本人身份签署合同' AFTER id;
