package com.emmanuelfinance.creditcard.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditCardProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_FAILED_TOPIC = "transaction-failed-topic";

    public void publishTransactionFailed(UUID transactionId) {
        log.info("Publicando evento de erro na transação Kafka ID: {}", transactionId);
        kafkaTemplate.send(TRANSACTION_FAILED_TOPIC, transactionId.toString());
    }
}