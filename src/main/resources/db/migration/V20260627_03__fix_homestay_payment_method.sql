-- 将民宿房源的按日支付改为押一付一
UPDATE house SET payment_method = '押一付一' WHERE payment_method = '按日支付';
