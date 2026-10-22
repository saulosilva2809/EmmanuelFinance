package com.emmanuelfinance.transaction.transaction.outbox;

import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.emmanuelfinance.transaction.transaction.kafka.TransactionProducer;
import com.emmanuelfinance.transaction.transaction.outbox.enums.TransactionEventOutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final TransactionProducer transactionProducer;

    @Override
    public boolean supports(OutboxEventType type) {
        for (TransactionEventOutboxEventType supported : TransactionEventOutboxEventType.values()) {
            if (supported.is(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        OutboxEventType type = outboxEvent.getType();
        JsonNode payload = outboxEvent.getPayload();

        if (TransactionEventOutboxEventType.TRANSACTION_CREATED.is(type)) {
            transactionProducer.publishTransactionCreated(objectMapper.treeToValue(payload, TransactionCreatedEvent.class));
        } else if (TransactionEventOutboxEventType.INVOICE_PAYMENT_CREATED.is(type)) {
            transactionProducer.publishInvoicePaymentCreated(objectMapper.treeToValue(payload, TransactionCreatedEvent.class));
        } else if (TransactionEventOutboxEventType.TRANSACTION_UPDATED.is(type)) {
            transactionProducer.publishTransactionUpdated(objectMapper.treeToValue(payload, TransactionUpdatedEvent.class));
        } else if (TransactionEventOutboxEventType.TRANSACTION_DELETED.is(type)) {
            transactionProducer.publishTransactionDeleted(objectMapper.treeToValue(payload, TransactionDeletedAndRestoreEvent.class));
        } else if (TransactionEventOutboxEventType.TRANSACTION_RESTORED.is(type)) {
            transactionProducer.publishTransactionRestore(objectMapper.treeToValue(payload, TransactionDeletedAndRestoreEvent.class));
        } else {
            throw new IllegalArgumentException("Tipo de evento não suportado por este dispatcher: " + type);
        }
    }
}
