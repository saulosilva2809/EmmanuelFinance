CREATE TABLE transfer (
                          id UUID PRIMARY KEY,
                          user_id UUID NOT NULL,
                          idempotency_key VARCHAR(255) NOT NULL,
                          source_account_id UUID,
                          destination_account_id UUID,
                          amount NUMERIC(15, 2) NOT NULL,
                          description VARCHAR(255),
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP,
                          deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Índices recomendados para performance e integridade de negócio
CREATE UNIQUE INDEX uk_transfer_idempotency_key ON transfer(idempotency_key);
CREATE INDEX idx_transfer_user_id ON transfer(user_id);