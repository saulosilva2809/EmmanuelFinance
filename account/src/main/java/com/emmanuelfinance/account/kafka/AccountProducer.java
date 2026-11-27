package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.shared.kafka.KafkaFailureHandler;
import com.emmanuelfinance.shared.modules.account.dto.AccountEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionFailedEvent;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferEvent;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountProducer implements KafkaFailureHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ACCOUNT_EVENTS_TOPIC = "account-events";
    private static final String TRANSACTION_FAILED_TOPIC = "transaction-failed-topic";
    private static final String TRANSFER_FAILED_TOPIC = "transfer-failed-topic";
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    public void publishAccountCreated(AccountEventDTO event) {
        log.info("Publicando evento de criação de conta no Kafka ID: {}", event.accountId());
        send(ACCOUNT_EVENTS_TOPIC, event.accountId(), event);
    }

    public void publishAccountDeleted(AccountEventDTO event) {
        log.info("Publicando evento de exclusão de conta no Kafka ID: {}", event.accountId());
        send(ACCOUNT_EVENTS_TOPIC, event.accountId(), event);
    }

    public void publishAccountRestored(AccountEventDTO event) {
        log.info("Publicando evento de restauração de conta no Kafka ID: {}", event.accountId());
        send(ACCOUNT_EVENTS_TOPIC, event.accountId(), event);
    }

    @Override
    public void onFailure(String topic, Object event) {
        if (event instanceof TransactionEvent transactionEvent) {
            log.info("Publicando evento de erro na transação Kafka ID: {}", transactionEvent.transactionId());
            send(TRANSACTION_FAILED_TOPIC, transactionEvent.transactionId(),
                    TransactionFailedEvent.from(topic, transactionEvent));
        } else if (event instanceof TransferEvent transferEvent) {
            log.info("Publicando evento de erro na transferência Kafka ID: {}", transferEvent.transferId());
            send(TRANSFER_FAILED_TOPIC, transferEvent.transferId(), TransferFailedEvent.from(topic, transferEvent));
        }
    }

    private void send(String topic, UUID key, Object event) {
        kafkaTemplate.send(topic, key.toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }
}
