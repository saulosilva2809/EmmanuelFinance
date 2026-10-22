package com.emmanuelfinance.transaction.outbox;

import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.outbox.enums.TransactionEventOutboxEventType;
import com.emmanuelfinance.transaction.outbox.enums.TransactionOutboxEventType;
import com.emmanuelfinance.transaction.query.TransactionDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventFactory {

    private final ObjectMapper objectMapper;
    private final TransactionMapper transactionMapper;

    public OutboxEvent transactionSnapshot(Transaction transaction, boolean deleted) {
        TransactionDocument snapshot = transactionMapper.toDocument(transaction);
        snapshot.setDeleted(deleted);

        return create(TransactionOutboxEventType.TRANSACTION_CHANGED, transaction.getId(), snapshot);
    }

    public OutboxEvent transactionCreated(TransactionCreatedEvent event) {
        return create(TransactionEventOutboxEventType.TRANSACTION_CREATED, event.transactionId(), event);
    }

    public OutboxEvent invoicePaymentCreated(TransactionCreatedEvent event) {
        return create(TransactionEventOutboxEventType.INVOICE_PAYMENT_CREATED, event.transactionId(), event);
    }

    public OutboxEvent transactionUpdated(TransactionUpdatedEvent event) {
        return create(TransactionEventOutboxEventType.TRANSACTION_UPDATED, event.transactionId(), event);
    }

    public OutboxEvent transactionDeleted(TransactionDeletedAndRestoreEvent event) {
        return create(TransactionEventOutboxEventType.TRANSACTION_DELETED, event.transactionId(), event);
    }

    public OutboxEvent transactionRestored(TransactionDeletedAndRestoreEvent event) {
        return create(TransactionEventOutboxEventType.TRANSACTION_RESTORED, event.transactionId(), event);
    }

    // o objeto vira json e vai para a coluna payload
    private OutboxEvent create(OutboxEventType type, UUID aggregateId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setType(type);
        outboxEvent.setAggregateId(aggregateId.toString());
        outboxEvent.setPayload(objectMapper.valueToTree(payload));
        outboxEvent.setCreatedAt(LocalDateTime.now());

        return outboxEvent;
    }
}
