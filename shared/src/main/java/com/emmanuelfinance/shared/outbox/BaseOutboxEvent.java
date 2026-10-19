package com.emmanuelfinance.shared.outbox;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Setter
@Getter
public abstract class BaseOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public abstract OutboxEventType getType();

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private JsonNode payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
