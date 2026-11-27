package com.emmanuelfinance.transaction.outbox;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventFactory {

    private final ObjectMapper objectMapper;

    // o objeto vira json e vai para a coluna payload
    public OutboxEvent create(OutboxEventType type, UUID aggregateId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setType(type);
        outboxEvent.setAggregateId(aggregateId.toString());
        outboxEvent.setPayload(objectMapper.valueToTree(payload));
        outboxEvent.setCreatedAt(LocalDateTime.now());

        return outboxEvent;
    }
}
