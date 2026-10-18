-- Outbox: tudo que precisa acontecer depois de salvar a conta (avisar o Kafka, atualizar o Mongo).
-- As linhas nascem na mesma transação da conta e são apagadas pelo OutboxRelay depois de processadas.
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_outbox_event_created_at ON outbox_event(created_at);
