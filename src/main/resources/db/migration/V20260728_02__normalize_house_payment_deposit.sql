UPDATE house
SET payment_method = CASE
    WHEN payment_method IS NULL OR TRIM(payment_method) = '' THEN '押一付一'
    WHEN payment_method IN ('无押金', '免押', '零押金') THEN '无押金月付'
    WHEN payment_method = '月付' THEN '押一付一'
    WHEN payment_method = '季付' THEN '押一付三'
    WHEN payment_method = '半年付' THEN '押一付六'
    WHEN payment_method IN ('年付', '押一付年') THEN '押一付十二'
    ELSE payment_method
END;

UPDATE house
SET deposit = CASE
    WHEN payment_method = '无押金月付' THEN 0
    WHEN payment_method IN ('押一付一', '押一付三', '押一付六', '押一付十二') THEN price
    WHEN payment_method = '押二付一' THEN price * 2
    ELSE deposit
END;
