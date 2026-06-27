-- 修复状态不一致：有活跃租约的房源应标记为已租
UPDATE house h
SET h.status = 'rented',
    h.updated_at = NOW()
WHERE h.id IN (
    SELECT l.house_id FROM lease l WHERE l.status = 'active'
)
AND h.status != 'rented';
