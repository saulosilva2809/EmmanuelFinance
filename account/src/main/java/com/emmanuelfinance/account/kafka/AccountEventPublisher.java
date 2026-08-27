package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventPublisher {

    private final KafkaTemplate<String, AccountEventDTO> kafkaTemplate;

    private static final String TOPIC = "account-events";

    public void publishAccount(AccountEventDTO event) {
        log.info("Publicando evento de conta no Kafka [Status: {}]: {}", event.status(), event.accountId());

        kafkaTemplate.send(TOPIC, event.accountId().toString(), event);
    }
}