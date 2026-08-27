ALTER TABLE transaction
DROP CONSTRAINT IF EXISTS uk_transaction_idempotency_key;

ALTER TABLE transaction
DROP CONSTRAINT IF EXISTS uk_transaction_natural_key;