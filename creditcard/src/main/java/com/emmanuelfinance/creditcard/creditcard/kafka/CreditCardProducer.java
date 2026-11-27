package com.emmanuelfinance.creditcard.creditcard.kafka;

import com.emmanuelfinance.shared.kafka.KafkaFailureHandler;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditCardProducer implements KafkaFailureHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_FAILED_TOPIC = "transaction-failed-topic";
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    @Override
    public void onFailure(String topic, Object event) {
        if (event instanceof TransactionEvent transactionEvent) {
            log.info("Publicando evento de erro na transação Kafka ID: {}", transactionEvent.transactionId());
            kafkaTemplate.send(TRANSACTION_FAILED_TOPIC, transactionEvent.transactionId().toString(),
                            TransactionFailedEvent.from(topic, transactionEvent))
                    .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        }
    }
}
