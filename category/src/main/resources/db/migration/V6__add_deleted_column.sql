-- 1. Adiciona a coluna deleted
ALTER TABLE category
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Remove o índice antigo (PostgreSQL)
DROP INDEX IF EXISTS idx_category_account_id;
DROP INDEX IF EXISTS idx_category_user_id;

-- 3. Cria o novo índice composto mais eficiente
CREATE INDEX idx_category_account_id_user_id_deleted ON category(account_id, user_id, deleted);