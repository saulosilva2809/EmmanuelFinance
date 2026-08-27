CREATE TABLE transaction (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    account_id UUID NOT NULL,
    credit_card_id UUID NOT NULL,
    category_id UUID NOT NULL,
    recurring_id UUID NOT NULL,
    description VARCHAR(255),
    amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT pk_transaction PRIMARY KEY (id)
);

CREATE INDEX idx_transaction_user_id_id ON transaction(user_id, id);
CREATE INDEX idx_transaction_user_deleted_id ON transaction(user_id, deleted, id);