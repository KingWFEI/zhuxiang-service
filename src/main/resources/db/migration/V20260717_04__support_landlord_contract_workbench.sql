-- 回填旧订单对应的房东系统用户，确保历史待签合同能够出现在房东工作台。
UPDATE rent_order ro
    INNER JOIN house h ON h.id = ro.house_id
    INNER JOIN landlord l ON l.id = h.landlord_id
SET ro.lessor_user_id = l.user_id
WHERE ro.lessor_user_id IS NULL
  AND l.user_id IS NOT NULL;

-- 房东工作台按当前房东和订单状态查询。
ALTER TABLE rent_order
    ADD INDEX idx_rent_order_lessor_status (lessor_user_id, status);

-- 房源绑定房东资料后，需要通过系统用户反查房东资料。
ALTER TABLE landlord
    ADD INDEX idx_landlord_user_id (user_id);
