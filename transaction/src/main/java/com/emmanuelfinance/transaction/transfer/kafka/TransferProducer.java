package com.emmanuelfinance.transaction.transfer.kafka;

import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferDeletedAndRestoreEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferUpdatedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSFER_CREATED_TOPIC = "transfer-created-topic";
    private static final String TRANSFER_UPDATED_TOPIC = "transfer-updated-topic";
    private static final String TRANSFER_DELETED_TOPIC = "transfer-deleted-topic";
    private static final String TRANSFER_RESTORE_TOPIC = "transfer-restore-topic";
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    public void publishTransferCreated(TransferCreatedEventDTO event) {
        send(TRANSFER_CREATED_TOPIC, event.transferId(), event);
    }

    public void publishTransferUpdated(TransferUpdatedEventDTO event) {
        send(TRANSFER_UPDATED_TOPIC, event.transferId(), event);
    }

    public void publishTransferDeleted(TransferDeletedAndRestoreEventDTO event) {
        send(TRANSFER_DELETED_TOPIC, event.transferId(), event);
    }

    public void publishTransferRestore(TransferDeletedAndRestoreEventDTO event) {
        send(TRANSFER_RESTORE_TOPIC, event.transferId(), event);
    }

    private void send(String topic, UUID transferId, Object event) {
        log.info("Publicando evento de transferência no tópico {} ID: {}", topic, transferId);
        kafkaTemplate.send(topic, transferId.toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }
}
