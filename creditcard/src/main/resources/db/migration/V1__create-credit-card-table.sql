CREATE TABLE credit_card (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    account_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    credit_limit NUMERIC(15, 2),
    closing_day INTEGER NOT NULL,
    due_day INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_credit_card_account_id ON credit_card(account_id);