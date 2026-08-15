-- Standardize every legacy table on the database default collation.
--
-- Each ALTER is guarded by information_schema so this migration can be
-- safely rerun after a MySQL DDL failure. CONVERT changes character columns
-- only; it does not delete or merge rows. Any unexpected unique-key conflict
-- makes MySQL stop the ALTER instead of discarding data.

-- The only foreign key touching these tables must be removed while both
-- participating varchar columns are converted, then restored unchanged.
SET @ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'landlord_auth_proof'
          AND constraint_name = 'fk_landlord_auth_proof_application'
    ),
    'ALTER TABLE landlord_auth_proof DROP FOREIGN KEY fk_landlord_auth_proof_application',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'customer_service_feedback'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE customer_service_feedback CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'customer_service_kb_document'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE customer_service_kb_document CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'customer_service_llm_log'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE customer_service_llm_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'customer_service_message'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE customer_service_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'customer_service_retrieval_log'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE customer_service_retrieval_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'customer_service_session'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE customer_service_session CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'deposit_deduction'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE deposit_deduction CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'deposit_deduction_item'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE deposit_deduction_item CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'deposit_record'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE deposit_record CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'house_inspection_template'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE house_inspection_template CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'immersive_image_hotspot'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE immersive_image_hotspot CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'immersive_scene'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE immersive_scene CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'immersive_scene_image'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE immersive_scene_image CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'immersive_tour'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE immersive_tour CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'inspection_photo'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE inspection_photo CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'landlord_auth_application'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE landlord_auth_application CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'landlord_auth_proof'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE landlord_auth_proof CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'lease_inspection_snapshot'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE lease_inspection_snapshot CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'lease_termination_applications'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE lease_termination_applications CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'lease_termination_logs'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE lease_termination_logs CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'payment_refund'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE payment_refund CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'repair_log'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE repair_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'repair_record'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE repair_record CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'user_real_name_auth'
             AND table_collation <> 'utf8mb4_0900_ai_ci'),
    'ALTER TABLE user_real_name_auth CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci',
    'SELECT 1'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'landlord_auth_proof'
          AND constraint_name = 'fk_landlord_auth_proof_application'
    ),
    'SELECT 1',
    'ALTER TABLE landlord_auth_proof ADD CONSTRAINT fk_landlord_auth_proof_application FOREIGN KEY (application_id) REFERENCES landlord_auth_application (id) ON DELETE CASCADE'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
