package com.emmanuelfinance.transaction.transfer.outbox;

import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferDeletedAndRestoreEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferUpdatedEventDTO;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.emmanuelfinance.transaction.outbox.OutboxEventFactory;
import com.emmanuelfinance.transaction.outbox.OutboxService;
import com.emmanuelfinance.transaction.transfer.Transfer;
import com.emmanuelfinance.transaction.transfer.TransferMapper;
import com.emmanuelfinance.transaction.transfer.outbox.enums.TransferEventOutboxEventType;
import com.emmanuelfinance.transaction.transfer.outbox.enums.TransferOutboxEventType;
import com.emmanuelfinance.transaction.transfer.query.TransferDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransferOutbox {

    private final OutboxService outboxService;
    private final OutboxEventFactory outboxEventFactory;
    private final TransferMapper transferMapper;

    public void transferChanged(Transfer transfer) {
        registerSnapshot(transfer, false);
    }

    public void transferDeleted(Transfer transfer) {
        registerSnapshot(transfer, true);
    }

    public void transferCreatedEvent(TransferCreatedEventDTO event) {
        register(TransferEventOutboxEventType.TRANSFER_CREATED, event.transferId(), event);
    }

    public void transferUpdatedEvent(TransferUpdatedEventDTO event) {
        register(TransferEventOutboxEventType.TRANSFER_UPDATED, event.transferId(), event);
    }

    public void transferDeletedEvent(TransferDeletedAndRestoreEventDTO event) {
        register(TransferEventOutboxEventType.TRANSFER_DELETED, event.transferId(), event);
    }

    public void transferRestoredEvent(TransferDeletedAndRestoreEventDTO event) {
        register(TransferEventOutboxEventType.TRANSFER_RESTORED, event.transferId(), event);
    }

    // a foto leva a transferência inteira, e a exclusão é marcada aqui porque a entidade apagada continua sem a marca
    private void registerSnapshot(Transfer transfer, boolean deleted) {
        TransferDocument snapshot = transferMapper.toDocument(transfer);
        snapshot.setDeleted(deleted);

        register(TransferOutboxEventType.TRANSFER_CHANGED, transfer.getId(), snapshot);
    }

    private void register(OutboxEventType type, UUID aggregateId, Object payload) {
        outboxService.register(outboxEventFactory.create(type, aggregateId, payload));
    }
}
