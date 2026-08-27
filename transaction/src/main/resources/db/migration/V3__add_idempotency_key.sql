ALTER TABLE transaction
    ALTER COLUMN credit_card_id DROP NOT NULL,
    ALTER COLUMN recurring_id DROP NOT NULL;

ALTER TABLE transaction
    ADD COLUMN idempotency_key VARCHAR(255) NOT NULL;

ALTER TABLE transaction
    ADD CONSTRAINT uk_transaction_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE transaction
    ADD CONSTRAINT uk_transaction_natural_key UNIQUE (account_id, amount, date, description);