package com.emmanuelfinance.transaction.outbox;

import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import com.emmanuelfinance.transaction.transaction.outbox.enums.TransactionOutboxEventType;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OutboxService outboxService;

    private final OutboxEventFactory outboxEventFactory = new OutboxEventFactory(new JsonMapper());

    @Test
    void shouldSaveTheRowAndThenWakeTheRelay() {
        OutboxEvent row = outboxEventFactory.create(
                TransactionOutboxEventType.TRANSACTION_CHANGED, UUID.randomUUID(), Map.of("campo", "valor"));

        outboxService.register(row);

        InOrder order = inOrder(outboxEventRepository, applicationEventPublisher);
        order.verify(outboxEventRepository).save(row);
        order.verify(applicationEventPublisher).publishEvent(any(OutboxEventSavedDTO.class));
    }

    @Test
    void shouldBuildTheRowFromThePayload() {
        UUID id = UUID.randomUUID();

        OutboxEvent row = outboxEventFactory.create(
                TransactionOutboxEventType.TRANSACTION_CHANGED, id, Map.of("campo", "valor"));

        assertEquals(TransactionOutboxEventType.TRANSACTION_CHANGED.name(), row.getType().name());
        assertEquals(id.toString(), row.getAggregateId());
        assertEquals("valor", row.getPayload().get("campo").asText());
        assertNotNull(row.getCreatedAt());
    }
}
