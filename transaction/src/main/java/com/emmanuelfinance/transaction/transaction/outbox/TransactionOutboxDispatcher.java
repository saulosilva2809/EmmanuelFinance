package com.emmanuelfinance.transaction.transaction.outbox;

import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.emmanuelfinance.transaction.transaction.outbox.enums.TransactionOutboxEventType;
import com.emmanuelfinance.transaction.transaction.query.TransactionDocument;
import com.emmanuelfinance.transaction.transaction.query.TransactionProjector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final TransactionProjector transactionProjector;

    @Override
    public boolean supports(OutboxEventType type) {
        return TransactionOutboxEventType.TRANSACTION_CHANGED.is(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        TransactionDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), TransactionDocument.class);
        transactionProjector.project(snapshot);
    }
}
