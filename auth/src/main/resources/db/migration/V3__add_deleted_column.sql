-- 1. Adiciona a coluna deleted
ALTER TABLE users
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Cria o novo índice composto mais eficiente
CREATE INDEX idx_user_deleted ON users(deleted);