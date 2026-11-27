package com.emmanuelfinance.transaction.transaction.outbox;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentFailedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.emmanuelfinance.transaction.outbox.OutboxEventFactory;
import com.emmanuelfinance.transaction.outbox.OutboxService;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.transaction.outbox.enums.TransactionEventOutboxEventType;
import com.emmanuelfinance.transaction.transaction.outbox.enums.TransactionOutboxEventType;
import com.emmanuelfinance.transaction.transaction.query.TransactionDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionOutbox {

    private final OutboxService outboxService;
    private final OutboxEventFactory outboxEventFactory;
    private final TransactionMapper transactionMapper;

    public void transactionChanged(Transaction transaction) {
        registerSnapshot(transaction, false);
    }

    public void transactionDeleted(Transaction transaction) {
        registerSnapshot(transaction, true);
    }

    public void transactionCreatedEvent(TransactionCreatedEvent event) {
        register(TransactionEventOutboxEventType.TRANSACTION_CREATED, event.transactionId(), event);
    }

    public void invoicePaymentCreatedEvent(TransactionCreatedEvent event) {
        register(TransactionEventOutboxEventType.INVOICE_PAYMENT_CREATED, event.transactionId(), event);
    }

    public void transactionUpdatedEvent(TransactionUpdatedEvent event) {
        register(TransactionEventOutboxEventType.TRANSACTION_UPDATED, event.transactionId(), event);
    }

    public void transactionDeletedEvent(TransactionDeletedAndRestoreEvent event) {
        register(TransactionEventOutboxEventType.TRANSACTION_DELETED, event.transactionId(), event);
    }

    public void transactionRestoredEvent(TransactionDeletedAndRestoreEvent event) {
        register(TransactionEventOutboxEventType.TRANSACTION_RESTORED, event.transactionId(), event);
    }

    public void invoicePaymentFailedEvent(InvoicePaymentFailedEvent event) {
        register(TransactionEventOutboxEventType.INVOICE_PAYMENT_FAILED, event.eventId(), event);
    }

    // a foto leva a transação inteira, e a exclusão é marcada aqui porque a entidade apagada continua sem a marca
    private void registerSnapshot(Transaction transaction, boolean deleted) {
        TransactionDocument snapshot = transactionMapper.toDocument(transaction);
        snapshot.setDeleted(deleted);

        register(TransactionOutboxEventType.TRANSACTION_CHANGED, transaction.getId(), snapshot);
    }

    private void register(OutboxEventType type, UUID aggregateId, Object payload) {
        outboxService.register(outboxEventFactory.create(type, aggregateId, payload));
    }
}
