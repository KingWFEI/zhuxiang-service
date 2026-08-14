CREATE TABLE recommendation_event (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NULL,
    anonymous_id VARCHAR(64) NULL,
    session_id VARCHAR(64) NULL,
    request_id VARCHAR(64) NULL,
    house_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    position_index INT NULL,
    duration_ms BIGINT NULL,
    source_page VARCHAR(32) NOT NULL DEFAULT 'home',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_recommendation_event_user_time (user_id, created_at),
    INDEX idx_recommendation_event_anonymous_time (anonymous_id, created_at),
    INDEX idx_recommendation_event_house_time (house_id, created_at),
    INDEX idx_recommendation_event_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Recommendation exposure and conversion events';

CREATE TABLE house_recommendation_stats (
    house_id VARCHAR(36) NOT NULL,
    exposure_count BIGINT NOT NULL DEFAULT 0,
    detail_click_count BIGINT NOT NULL DEFAULT 0,
    effective_view_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    appointment_count BIGINT NOT NULL DEFAULT 0,
    order_count BIGINT NOT NULL DEFAULT 0,
    not_interested_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (house_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Aggregated recommendation statistics by house';
