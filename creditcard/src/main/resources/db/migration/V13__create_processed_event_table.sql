CREATE TABLE processed_event (
    event_id     UUID      NOT NULL,
    processed_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_processed_event PRIMARY KEY (event_id)
);
