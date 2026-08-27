package com.emmanuelfinance.transaction.kafka;

import com.emmanuelfinance.shared.modules.transaction.kafka.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.TransactionUpdatedEvent;
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

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        log.info("Publicando evento de criacao de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_CREATED_TOPIC, event.transactionId().toString(), event);
    }

    public void publishTransactionUpdated(TransactionUpdatedEvent event) {
        log.info("Publicando evento de atualizacao de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_UPDATED_TOPIC, event.transactionId().toString(), event);
    }
}