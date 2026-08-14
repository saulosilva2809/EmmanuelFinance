-- 1. Adiciona a coluna deleted
ALTER TABLE account
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Remove o índice antigo (PostgreSQL)
DROP INDEX IF EXISTS idx_account_user_id;

-- 3. Cria o novo índice composto mais eficiente
CREATE INDEX idx_account_user_deleted ON account(user_id, deleted);