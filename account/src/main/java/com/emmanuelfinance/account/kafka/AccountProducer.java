package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.shared.modules.account.dto.AccountEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountProducer {

    private final KafkaTemplate<String, AccountEventDTO> kafkaTemplate;

    private static final String ACCOUNT_EVENTS_TOPIC = "account-events";
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    public void publishAccountCreated(AccountEventDTO event) {
        log.info("Publicando evento de criação de conta no Kafka ID: {}", event.accountId());
        send(event);
    }

    public void publishAccountDeleted(AccountEventDTO event) {
        log.info("Publicando evento de exclusão de conta no Kafka ID: {}", event.accountId());
        send(event);
    }

    public void publishAccountRestored(AccountEventDTO event) {
        log.info("Publicando evento de restauração de conta no Kafka ID: {}", event.accountId());
        send(event);
    }

    private void send(AccountEventDTO event) {
        kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, event.accountId().toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }
}
