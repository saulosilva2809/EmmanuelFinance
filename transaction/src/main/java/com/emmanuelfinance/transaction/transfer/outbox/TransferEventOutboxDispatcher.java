package com.emmanuelfinance.transaction.transfer.outbox;

import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferDeletedAndRestoreEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferUpdatedEventDTO;
import com.emmanuelfinance.transaction.transfer.outbox.enums.TransferEventOutboxEventType;
import com.emmanuelfinance.transaction.transfer.kafka.TransferProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferEventOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final TransferProducer transferProducer;

    @Override
    public boolean supports(OutboxEventType type) {
        for (TransferEventOutboxEventType supported : TransferEventOutboxEventType.values()) {
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

        if (TransferEventOutboxEventType.TRANSFER_CREATED.is(type)) {
            transferProducer.publishTransferCreated(objectMapper.treeToValue(payload, TransferCreatedEventDTO.class));
        } else if (TransferEventOutboxEventType.TRANSFER_UPDATED.is(type)) {
            transferProducer.publishTransferUpdated(objectMapper.treeToValue(payload, TransferUpdatedEventDTO.class));
        } else if (TransferEventOutboxEventType.TRANSFER_DELETED.is(type)) {
            transferProducer.publishTransferDeleted(objectMapper.treeToValue(payload, TransferDeletedAndRestoreEventDTO.class));
        } else if (TransferEventOutboxEventType.TRANSFER_RESTORED.is(type)) {
            transferProducer.publishTransferRestore(objectMapper.treeToValue(payload, TransferDeletedAndRestoreEventDTO.class));
        } else {
            throw new IllegalArgumentException("Tipo de evento não suportado por este dispatcher: " + type);
        }
    }
}
