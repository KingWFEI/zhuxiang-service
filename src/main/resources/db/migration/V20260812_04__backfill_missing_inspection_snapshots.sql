-- A lease without an inspection template still needs an empty snapshot.
-- Existing templates are copied; otherwise [] means no photos are required.
INSERT INTO lease_inspection_snapshot
    (id, contract_id, lease_id, house_id, template_version, rooms, status, created_at, updated_at)
SELECT UUID(),
       lease.contract_id,
       lease.id,
       lease.house_id,
       COALESCE(template.version, 0),
       COALESCE(template.rooms, JSON_ARRAY()),
       'DRAFT',
       NOW(),
       NOW()
FROM lease
LEFT JOIN house_inspection_template template ON template.house_id = lease.house_id
LEFT JOIN lease_inspection_snapshot snapshot ON snapshot.contract_id = lease.contract_id
WHERE lease.contract_id IS NOT NULL
  AND snapshot.id IS NULL;
