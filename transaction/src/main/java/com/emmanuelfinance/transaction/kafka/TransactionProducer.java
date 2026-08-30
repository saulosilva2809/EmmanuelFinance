package com.emmanuelfinance.transaction.kafka;

import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_CREATED_TOPIC = "transaction-created-topic";
    private static final String TRANSACTION_UPDATED_TOPIC = "transaction-updated-topic";
    private static final String TRANSACTION_DELETED_TOPIC = "transaction-deleted-topic";
    private static final String TRANSACTION_RESTORE_TOPIC = "transaction-restore-topic";

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        log.info("Publicando evento de criacao de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_CREATED_TOPIC, event.transactionId().toString(), event);
    }

    public void publishTransactionUpdated(TransactionUpdatedEvent event) {
        log.info("Publicando evento de atualizacao de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_UPDATED_TOPIC, event.transactionId().toString(), event);
    }

    public void publishTransactionDeleted(TransactionDeletedAndRestoreEvent event) {
        log.info("Publicando evento de exclusão de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_DELETED_TOPIC, event.transactionId().toString(), event);
    }

    public void publishTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        log.info("Publicando evento de recuperação de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_RESTORE_TOPIC, event.transactionId().toString(), event);
    }
}