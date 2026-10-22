package com.emmanuelfinance.transaction.outbox;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.outbox.enums.TransactionEventOutboxEventType;
import com.emmanuelfinance.transaction.outbox.enums.TransactionOutboxEventType;
import com.emmanuelfinance.transaction.query.TransactionDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        OutboxEventFactory outboxEventFactory =
                new OutboxEventFactory(objectMapper, Mappers.getMapper(TransactionMapper.class));
        outboxService = new OutboxService(outboxEventRepository, applicationEventPublisher, outboxEventFactory);
    }

    @Test
    void shouldRegisterTheSnapshotWhenTheTransactionChanges() throws Exception {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());

        outboxService.transactionChanged(transaction);

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(TransactionOutboxEventType.TRANSACTION_CHANGED.name(), saved.get(0).getType().name());
        assertFalse(objectMapper.treeToValue(saved.get(0).getPayload(), TransactionDocument.class).isDeleted());
    }

    @Test
    void shouldRegisterTheSnapshotMarkedAsDeletedWhenTheTransactionIsDeleted() throws Exception {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());

        outboxService.transactionDeleted(transaction);

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertTrue(objectMapper.treeToValue(saved.get(0).getPayload(), TransactionDocument.class).isDeleted());
    }

    @Test
    void shouldRegisterOneRowForEachKindOfKafkaEvent() {
        TransactionCreatedEvent created = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), new BigDecimal("10.00"),
                1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now()
        );
        TransactionUpdatedEvent updated = new TransactionUpdatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, TypeEnum.EXPENSE, TypeEnum.EXPENSE,
                StatusTransactionEnum.PAID, StatusTransactionEnum.PAID
        );
        TransactionDeletedAndRestoreEvent deleted = new TransactionDeletedAndRestoreEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), BigDecimal.TEN,
                1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now()
        );

        outboxService.transactionCreatedEvent(created);
        outboxService.invoicePaymentCreatedEvent(created);
        outboxService.transactionUpdatedEvent(updated);
        outboxService.transactionDeletedEvent(deleted);
        outboxService.transactionRestoredEvent(deleted);

        ArgumentCaptor<List<OutboxEvent>> captor = rowsCaptor();
        verify(outboxEventRepository, times(5)).saveAll(captor.capture());
        List<String> types = captor.getAllValues().stream().map(rows -> rows.get(0).getType().name()).toList();
        assertEquals(List.of(
                TransactionEventOutboxEventType.TRANSACTION_CREATED.name(),
                TransactionEventOutboxEventType.INVOICE_PAYMENT_CREATED.name(),
                TransactionEventOutboxEventType.TRANSACTION_UPDATED.name(),
                TransactionEventOutboxEventType.TRANSACTION_DELETED.name(),
                TransactionEventOutboxEventType.TRANSACTION_RESTORED.name()
        ), types);
    }

    @Test
    void shouldWakeTheRelayOncePerOperation() {
        outboxService.transactionChanged(TransactionTestDataBuilder.transactionEntity(UUID.randomUUID()));

        verify(applicationEventPublisher, times(1)).publishEvent(any(OutboxEventSavedDTO.class));
    }

    private List<OutboxEvent> registeredRows() {
        ArgumentCaptor<List<OutboxEvent>> captor = rowsCaptor();
        verify(outboxEventRepository, times(1)).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<OutboxEvent>> rowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
