ALTER TABLE customer_service_message
    ADD COLUMN sequence_no BIGINT NULL COMMENT '会话内消息顺序号' AFTER session_id;

UPDATE customer_service_message AS target
JOIN (
    SELECT ranked_inner.id, ranked_inner.generated_sequence
    FROM (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY session_id
                ORDER BY
                    created_at ASC,
                    CASE role
                        WHEN 'USER' THEN 0
                        WHEN 'ASSISTANT' THEN 1
                        ELSE 2
                    END ASC,
                    id ASC
            ) AS generated_sequence
        FROM customer_service_message
    ) AS ranked_inner
) AS ranked ON ranked.id = target.id
SET target.sequence_no = ranked.generated_sequence;

ALTER TABLE customer_service_message
    MODIFY COLUMN sequence_no BIGINT NOT NULL COMMENT '会话内消息顺序号',
    ADD UNIQUE KEY uk_cs_msg_session_sequence (session_id, sequence_no);
