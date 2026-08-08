CREATE INDEX idx_message_user_unread_category
    ON message (user_id, is_deleted, is_read, category);
