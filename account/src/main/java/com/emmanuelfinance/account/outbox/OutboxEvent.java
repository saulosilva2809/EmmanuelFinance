package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // define o tipo de evento
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OutboxEventType type;

    // id da conta
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    // content da conta
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private JsonNode payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
