DROP INDEX IF EXISTS idx_category_account_id_user_id_deleted;

ALTER TABLE category DROP COLUMN IF EXISTS account_id;