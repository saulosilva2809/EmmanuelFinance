package com.emmanuelfinance.transaction.transaction.outbox;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.outbox.OutboxEvent;
import com.emmanuelfinance.transaction.outbox.OutboxEventFactory;
import com.emmanuelfinance.transaction.outbox.OutboxService;
import com.emmanuelfinance.transaction.transaction.outbox.enums.TransactionEventOutboxEventType;
import com.emmanuelfinance.transaction.transaction.outbox.enums.TransactionOutboxEventType;
import com.emmanuelfinance.transaction.transaction.query.TransactionDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionOutboxTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    @Mock
    private OutboxService outboxService;

    private TransactionOutbox transactionOutbox;

    @BeforeEach
    void setUp() {
        transactionOutbox = new TransactionOutbox(
                outboxService, new OutboxEventFactory(objectMapper), Mappers.getMapper(TransactionMapper.class));
    }

    @Test
    void shouldBuildTheSnapshotWithTheWholeTransaction() throws Exception {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());

        transactionOutbox.transactionChanged(transaction);
        OutboxEvent row = registeredRows(1).get(0);

        assertEquals(TransactionOutboxEventType.TRANSACTION_CHANGED.name(), row.getType().name());
        assertEquals(transaction.getId().toString(), row.getAggregateId());
        assertNotNull(row.getCreatedAt());

        TransactionDocument snapshot = objectMapper.treeToValue(row.getPayload(), TransactionDocument.class);
        assertEquals(transaction.getId(), snapshot.getId());
        assertEquals(transaction.getUserId(), snapshot.getUserId());
        assertEquals(transaction.getAccountId(), snapshot.getAccountId());
        assertEquals(transaction.getCategoryId(), snapshot.getCategoryId());
        assertEquals(transaction.getDescription(), snapshot.getDescription());
        assertEquals(0, transaction.getAmount().compareTo(snapshot.getAmount()));
        assertEquals(transaction.getStatus(), snapshot.getStatus());
        assertEquals(transaction.getType(), snapshot.getType());
        assertEquals(transaction.isScheduled(), snapshot.isScheduled());
        assertFalse(snapshot.isDeleted());
    }

    @Test
    void shouldMarkTheSnapshotAsDeletedWhenAskedTo() throws Exception {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());

        transactionOutbox.transactionDeleted(transaction);
        OutboxEvent row = registeredRows(1).get(0);

        assertTrue(objectMapper.treeToValue(row.getPayload(), TransactionDocument.class).isDeleted());
    }

    @Test
    void shouldBuildTheCreatedEventRowWithTheEventAsPayload() throws Exception {
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), new BigDecimal("100.00"),
                1, TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.of(2026, 10, 5, 10, 0), UUID.randomUUID()
        );

        transactionOutbox.transactionCreatedEvent(event);
        transactionOutbox.invoicePaymentCreatedEvent(event);
        List<OutboxEvent> rows = registeredRows(2);
        OutboxEvent created = rows.get(0);
        OutboxEvent invoicePayment = rows.get(1);

        assertEquals(TransactionEventOutboxEventType.TRANSACTION_CREATED.name(), created.getType().name());
        assertEquals(TransactionEventOutboxEventType.INVOICE_PAYMENT_CREATED.name(), invoicePayment.getType().name());
        assertEquals(event.transactionId().toString(), created.getAggregateId());

        TransactionCreatedEvent saved = objectMapper.treeToValue(created.getPayload(), TransactionCreatedEvent.class);
        assertEquals(event.transactionId(), saved.transactionId());
        assertEquals(event.accountId(), saved.accountId());
        assertEquals(event.userId(), saved.userId());
        assertEquals(event.type(), saved.type());
        assertEquals(event.date(), saved.date());
        assertEquals(0, event.amount().compareTo(saved.amount()));
    }

    @Test
    void shouldBuildTheUpdatedEventRow() throws Exception {
        TransactionUpdatedEvent event = new TransactionUpdatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), new BigDecimal("150.00"), TypeTransactionEnum.EXPENSE, TypeTransactionEnum.INCOME,
                StatusTransactionEnum.PAID, StatusTransactionEnum.PAID, UUID.randomUUID()
        );

        transactionOutbox.transactionUpdatedEvent(event);
        OutboxEvent row = registeredRows(1).get(0);

        assertEquals(TransactionEventOutboxEventType.TRANSACTION_UPDATED.name(), row.getType().name());
        TransactionUpdatedEvent saved = objectMapper.treeToValue(row.getPayload(), TransactionUpdatedEvent.class);
        assertEquals(event.oldAccountId(), saved.oldAccountId());
        assertEquals(event.newAccountId(), saved.newAccountId());
        assertEquals(event.oldType(), saved.oldType());
        assertEquals(event.newType(), saved.newType());
    }

    @Test
    void shouldBuildTheDeletedAndRestoredEventRows() {
        TransactionDeletedAndRestoreEvent event = new TransactionDeletedAndRestoreEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), new BigDecimal("100.00"),
                1, TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID()
        );

        transactionOutbox.transactionDeletedEvent(event);
        transactionOutbox.transactionRestoredEvent(event);
        List<OutboxEvent> rows = registeredRows(2);
        OutboxEvent deleted = rows.get(0);
        OutboxEvent restored = rows.get(1);

        assertEquals(TransactionEventOutboxEventType.TRANSACTION_DELETED.name(), deleted.getType().name());
        assertEquals(TransactionEventOutboxEventType.TRANSACTION_RESTORED.name(), restored.getType().name());
        assertEquals(event.transactionId().toString(), restored.getAggregateId());
    }

    private List<OutboxEvent> registeredRows(int expected) {
        ArgumentCaptor<OutboxEvent> rows = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService, times(expected)).register(rows.capture());
        return rows.getAllValues();
    }
}
