package com.emmanuelfinance.transaction.outbox;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.transaction.kafka.TransactionProducer;
import com.emmanuelfinance.transaction.outbox.enums.TransactionEventOutboxEventType;
import com.emmanuelfinance.transaction.outbox.enums.TransactionOutboxEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventOutboxDispatcherTest {

    @Mock
    private TransactionProducer transactionProducer;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private TransactionEventOutboxDispatcher dispatcher;

    private final TransactionCreatedEvent created = new TransactionCreatedEvent(
            UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), new BigDecimal("100.00"),
            1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.of(2026, 10, 5, 10, 0)
    );

    private final TransactionDeletedAndRestoreEvent deleted = new TransactionDeletedAndRestoreEvent(
            UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), new BigDecimal("100.00"),
            1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.of(2026, 10, 5, 10, 0)
    );

    @Test
    void shouldSupportAllTheKafkaEventsAndNothingElse() {
        for (TransactionEventOutboxEventType type : TransactionEventOutboxEventType.values()) {
            assertTrue(dispatcher.supports(type), type.name());
        }
        assertFalse(dispatcher.supports(TransactionOutboxEventType.TRANSACTION_CHANGED));
        assertFalse(dispatcher.supports(() -> "OUTRO_TIPO"));
    }

    @Test
    void shouldPublishTheCreatedEvent() throws Exception {
        dispatcher.dispatch(row(TransactionEventOutboxEventType.TRANSACTION_CREATED, created));

        verify(transactionProducer).publishTransactionCreated(argThat(received ->
                received.transactionId().equals(created.transactionId())
                        && received.amount().compareTo(new BigDecimal("100.00")) == 0
                        && received.date().equals(created.date())));
        verifyNoMoreInteractions(transactionProducer);
    }

    @Test
    void shouldPublishTheInvoicePaymentEventInItsOwnTopic() throws Exception {
        dispatcher.dispatch(row(TransactionEventOutboxEventType.INVOICE_PAYMENT_CREATED, created));

        verify(transactionProducer).publishInvoicePaymentCreated(argThat(received ->
                received.transactionId().equals(created.transactionId())));
        verifyNoMoreInteractions(transactionProducer);
    }

    @Test
    void shouldPublishTheUpdatedEvent() throws Exception {
        TransactionUpdatedEvent updated = new TransactionUpdatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, TypeEnum.EXPENSE, TypeEnum.INCOME,
                StatusTransactionEnum.PAID, StatusTransactionEnum.PAID
        );

        dispatcher.dispatch(row(TransactionEventOutboxEventType.TRANSACTION_UPDATED, updated));

        verify(transactionProducer).publishTransactionUpdated(argThat(received ->
                received.transactionId().equals(updated.transactionId())
                        && received.newType() == TypeEnum.INCOME));
    }

    @Test
    void shouldPublishTheDeletedAndRestoredEventsEachInItsTopic() throws Exception {
        dispatcher.dispatch(row(TransactionEventOutboxEventType.TRANSACTION_DELETED, deleted));
        dispatcher.dispatch(row(TransactionEventOutboxEventType.TRANSACTION_RESTORED, deleted));

        verify(transactionProducer).publishTransactionDeleted(argThat(received ->
                received.transactionId().equals(deleted.transactionId())));
        verify(transactionProducer).publishTransactionRestore(argThat(received ->
                received.transactionId().equals(deleted.transactionId())));
    }

    @Test
    void shouldLetTheFailureOfKafkaGoUpSoTheRowStaysPending() {
        doThrow(new IllegalStateException("kafka fora do ar")).when(transactionProducer).publishTransactionCreated(any());

        assertThrows(IllegalStateException.class,
                () -> dispatcher.dispatch(row(TransactionEventOutboxEventType.TRANSACTION_CREATED, created)));
    }

    private OutboxEvent row(TransactionEventOutboxEventType type, Object payload) {
        OutboxEvent row = new OutboxEvent();
        row.setType(type);
        row.setAggregateId(UUID.randomUUID().toString());
        row.setPayload(objectMapper.valueToTree(payload));
        return row;
    }
}
