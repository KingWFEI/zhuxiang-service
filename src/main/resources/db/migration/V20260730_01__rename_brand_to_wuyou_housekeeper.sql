UPDATE `user`
SET nickname = '勿忧管家用户',
    updated_at = CURRENT_TIMESTAMP
WHERE nickname = '住享用户';

UPDATE `user`
SET nickname = '勿忧管家',
    updated_at = CURRENT_TIMESTAMP
WHERE nickname = '住享平台';

UPDATE landlord
SET name = '勿忧管家',
    updated_at = CURRENT_TIMESTAMP
WHERE name = '住享平台';

UPDATE message
SET title = '欢迎使用勿忧管家'
WHERE title = '欢迎使用住享';

UPDATE lock_device
SET lock_brand = '勿忧管家智能锁',
    updated_at = CURRENT_TIMESTAMP
WHERE lock_brand = '住享智能锁';
