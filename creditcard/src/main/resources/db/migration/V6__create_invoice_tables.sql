CREATE TABLE invoice (
                         id UUID PRIMARY KEY,
                         credit_card_id UUID NOT NULL,
                         user_id UUID NOT NULL,
                         month INT NOT NULL,
                         year INT NOT NULL,
                         due_date DATE NOT NULL,
                         closing_date DATE NOT NULL,
                         total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
                         status VARCHAR(50) NOT NULL,
                         deleted BOOLEAN NOT NULL DEFAULT FALSE,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT uk_invoice_card_month_year UNIQUE (credit_card_id, month, year),
                         CONSTRAINT chk_invoice_month CHECK (month BETWEEN 1 AND 12)
    );

CREATE INDEX idx_invoice_credit_card_id ON invoice(credit_card_id);
CREATE INDEX idx_invoice_lookup ON invoice(credit_card_id, month, year) WHERE deleted = FALSE;

CREATE TABLE invoice_item (
                              id UUID PRIMARY KEY,
                              invoice_id UUID NOT NULL,
                              user_id UUID NOT NULL,
                              transaction_id UUID NOT NULL,
                              installment_number INT NOT NULL,
                              total_installments INT NOT NULL,
                              amount NUMERIC(15, 2) NOT NULL,
                              deleted BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_invoice_item_invoice FOREIGN KEY (invoice_id)
                                  REFERENCES invoice(id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_item_invoice_id ON invoice_item(invoice_id);
CREATE INDEX idx_invoice_item_transaction_id ON invoice_item(transaction_id);