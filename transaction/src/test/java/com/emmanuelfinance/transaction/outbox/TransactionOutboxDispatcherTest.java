package com.emmanuelfinance.transaction.outbox;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.outbox.enums.TransactionEventOutboxEventType;
import com.emmanuelfinance.transaction.outbox.enums.TransactionOutboxEventType;
import com.emmanuelfinance.transaction.query.TransactionDocument;
import com.emmanuelfinance.transaction.query.TransactionProjector;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionOutboxDispatcherTest {

    @Mock
    private TransactionProjector transactionProjector;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private TransactionOutboxDispatcher dispatcher;

    @Test
    void shouldSupportOnlyTheTransactionChangedType() {
        assertTrue(dispatcher.supports(TransactionOutboxEventType.TRANSACTION_CHANGED));
        assertFalse(dispatcher.supports(TransactionEventOutboxEventType.TRANSACTION_CREATED));
    }

    @Test
    void shouldRecognizeTheTypeReadBackFromTheDatabase() {
        assertTrue(dispatcher.supports(row(newSnapshot()).getType()));
    }

    @Test
    void shouldApplyTheSnapshotInMongo() throws Exception {
        TransactionDocument snapshot = newSnapshot();

        dispatcher.dispatch(row(snapshot));

        verify(transactionProjector).project(argThat(received ->
                received.getId().equals(snapshot.getId())
                        && received.getAmount().compareTo(new BigDecimal("150.50")) == 0
                        && received.getStatus() == StatusTransactionEnum.PAID
                        && received.getDate().equals(snapshot.getDate())
                        && received.isDeleted()));
    }

    @Test
    void shouldLetTheFailureOfMongoGoUpSoTheRowStaysPending() {
        doThrow(new IllegalStateException("mongo fora do ar")).when(transactionProjector).project(any());

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(row(newSnapshot())));
    }

    private TransactionDocument newSnapshot() {
        TransactionDocument snapshot = new TransactionDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setUserId(UUID.randomUUID());
        snapshot.setAccountId(UUID.randomUUID());
        snapshot.setAmount(new BigDecimal("150.50"));
        snapshot.setStatus(StatusTransactionEnum.PAID);
        snapshot.setType(TypeEnum.EXPENSE);
        snapshot.setDate(LocalDateTime.of(2026, 10, 5, 10, 30));
        snapshot.setDeleted(true);
        return snapshot;
    }

    private OutboxEvent row(TransactionDocument snapshot) {
        OutboxEvent row = new OutboxEvent();
        row.setType(TransactionOutboxEventType.TRANSACTION_CHANGED);
        row.setAggregateId(snapshot.getId().toString());
        row.setPayload(objectMapper.valueToTree(snapshot));
        return row;
    }
}
