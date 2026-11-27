package com.emmanuelfinance.transaction.transfer.outbox;

import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.emmanuelfinance.transaction.transfer.outbox.enums.TransferOutboxEventType;
import com.emmanuelfinance.transaction.transfer.query.TransferDocument;
import com.emmanuelfinance.transaction.transfer.query.TransferProjector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final TransferProjector transferProjector;

    @Override
    public boolean supports(OutboxEventType type) {
        return TransferOutboxEventType.TRANSFER_CHANGED.is(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        TransferDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), TransferDocument.class);
        transferProjector.project(snapshot);
    }
}
