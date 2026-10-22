-- Outbox: eventos que precisam ser entregues depois de salvar a transação (Mongo e Kafka).
-- As linhas nascem na mesma transação do dado e são apagadas pelo OutboxRelay depois de entregues.
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_outbox_event_created_at ON outbox_event(created_at);
