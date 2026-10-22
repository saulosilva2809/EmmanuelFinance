package com.emmanuelfinance.transaction.outbox;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.outbox.enums.TransactionEventOutboxEventType;
import com.emmanuelfinance.transaction.outbox.enums.TransactionOutboxEventType;
import com.emmanuelfinance.transaction.query.TransactionDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventFactoryTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final OutboxEventFactory outboxEventFactory =
            new OutboxEventFactory(objectMapper, Mappers.getMapper(TransactionMapper.class));

    @Test
    void shouldBuildTheSnapshotWithTheWholeTransaction() throws Exception {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());

        OutboxEvent row = outboxEventFactory.transactionSnapshot(transaction, false);

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

        OutboxEvent row = outboxEventFactory.transactionSnapshot(transaction, true);

        assertTrue(objectMapper.treeToValue(row.getPayload(), TransactionDocument.class).isDeleted());
    }

    @Test
    void shouldBuildTheCreatedEventRowWithTheEventAsPayload() throws Exception {
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), new BigDecimal("100.00"),
                1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.of(2026, 10, 5, 10, 0)
        );

        OutboxEvent created = outboxEventFactory.transactionCreated(event);
        OutboxEvent invoicePayment = outboxEventFactory.invoicePaymentCreated(event);

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
                new BigDecimal("100.00"), new BigDecimal("150.00"), TypeEnum.EXPENSE, TypeEnum.INCOME,
                StatusTransactionEnum.PAID, StatusTransactionEnum.PAID
        );

        OutboxEvent row = outboxEventFactory.transactionUpdated(event);

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
                1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now()
        );

        OutboxEvent deleted = outboxEventFactory.transactionDeleted(event);
        OutboxEvent restored = outboxEventFactory.transactionRestored(event);

        assertEquals(TransactionEventOutboxEventType.TRANSACTION_DELETED.name(), deleted.getType().name());
        assertEquals(TransactionEventOutboxEventType.TRANSACTION_RESTORED.name(), restored.getType().name());
        assertEquals(event.transactionId().toString(), restored.getAggregateId());
    }
}
