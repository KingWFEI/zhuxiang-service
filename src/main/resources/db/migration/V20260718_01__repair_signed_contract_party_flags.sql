-- 已签署或签署后终止的合同，双方签署标记必须与合同生命周期状态保持一致。
UPDATE rent_contract
SET lessor_signed = 1,
    tenant_signed = 1
WHERE (status = 'signed' OR (status = 'terminated' AND signed_at IS NOT NULL))
  AND (lessor_signed <> 1 OR tenant_signed <> 1);
