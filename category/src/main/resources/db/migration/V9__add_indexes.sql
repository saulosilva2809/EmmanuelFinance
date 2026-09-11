DROP INDEX IF EXISTS idx_category_account_id_user_id_deleted;

CREATE INDEX idx_category_user_deleted_created
    ON category (user_id, deleted, created_at DESC);