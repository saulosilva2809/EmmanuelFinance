-- 1. Adiciona a coluna deleted
ALTER TABLE credit_card
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Remove o índice antigo (PostgreSQL)
DROP INDEX IF EXISTS idx_credit_card_account_id;

-- 3. Cria o novo índice composto mais eficiente
CREATE INDEX idx_credit_card_account_id_user_id_deleted ON credit_card(account_id, user_id, deleted);