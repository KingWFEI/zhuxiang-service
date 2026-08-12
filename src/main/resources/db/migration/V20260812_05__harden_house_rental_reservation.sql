-- MySQL DDL is not transactional. IF NOT EXISTS/IGNORE make this migration safe to
-- rerun after Flyway repair when an earlier ALTER TABLE failed halfway through.
CREATE TABLE IF NOT EXISTS house_rental_reservation (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    house_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL COMMENT 'ACTIVE/RELEASED/CONVERTED',
    expires_at TIMESTAMP NULL,
    active_house_id VARCHAR(36)
        GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN house_id ELSE NULL END) STORED,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_house_active_reservation (active_house_id),
    UNIQUE KEY uk_reservation_order (order_id),
    KEY idx_reservation_house_status (house_id, status),
    KEY idx_reservation_user_status (user_id, status)
) COMMENT='租房订单对房源的排他占用';

INSERT IGNORE INTO house_rental_reservation
    (id, house_id, order_id, user_id, status, expires_at, created_at, updated_at)
SELECT UUID(), h.id, ro.id, ro.user_id, 'ACTIVE', h.reserved_until,
       COALESCE(ro.created_at, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP
FROM house h JOIN rent_order ro ON ro.id = h.reserved_order_id
WHERE h.reserved_order_id IS NOT NULL
  AND ro.status IN ('created', 'pendingRealName', 'pendingContract', 'pendingPayment',
                    'pendingTenantSign', 'pendingLandlordSign');

-- Historical versions allowed repeated payment attempts. Preserve every record,
-- but close stale pending attempts before adding the invariant:
--   1) if an order has a success record, every pending attempt is obsolete;
--   2) otherwise keep only the newest pending attempt active.
UPDATE payment_record pending_record
JOIN payment_record success_record
  ON success_record.order_id = pending_record.order_id
 AND success_record.type = 'rent'
 AND success_record.status = 'success'
SET pending_record.status = 'failed',
    pending_record.remark = CONCAT_WS('; ', NULLIF(pending_record.remark, ''),
                                      'migration: superseded by successful rent payment'),
    pending_record.updated_at = CURRENT_TIMESTAMP
WHERE pending_record.type = 'rent'
  AND pending_record.status = 'pending';

UPDATE payment_record older
JOIN payment_record newer
  ON newer.order_id = older.order_id
 AND newer.type = 'rent'
 AND newer.status = 'pending'
 AND (newer.created_at > older.created_at
      OR (newer.created_at = older.created_at AND newer.id > older.id))
SET older.status = 'failed',
    older.remark = CONCAT_WS('; ', NULLIF(older.remark, ''),
                             'migration: duplicate pending rent payment closed'),
    older.updated_at = CURRENT_TIMESTAMP
WHERE older.type = 'rent'
  AND older.status = 'pending';

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'payment_record'
             AND column_name = 'active_rent_order_id'),
    'SELECT 1',
    'ALTER TABLE payment_record ADD COLUMN active_rent_order_id VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN type = ''rent'' AND status IN (''pending'', ''success'') THEN order_id ELSE NULL END) STORED'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'payment_record'
             AND index_name = 'uk_payment_active_rent_order'),
    'SELECT 1',
    'ALTER TABLE payment_record ADD UNIQUE KEY uk_payment_active_rent_order (active_rent_order_id)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.columns
           WHERE table_schema = DATABASE() AND table_name = 'lease'
             AND column_name = 'effective_house_id'),
    'SELECT 1',
    'ALTER TABLE lease ADD COLUMN effective_house_id VARCHAR(36) GENERATED ALWAYS AS (CASE WHEN UPPER(status) IN (''ACTIVE'', ''EFFECTIVE'', ''PENDING'', ''PENDING_EFFECTIVE'') THEN house_id ELSE NULL END) STORED'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.statistics
           WHERE table_schema = DATABASE() AND table_name = 'lease'
             AND index_name = 'uk_lease_effective_house'),
    'SELECT 1',
    'ALTER TABLE lease ADD UNIQUE KEY uk_lease_effective_house (effective_house_id)'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
