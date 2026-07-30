package com.emmanuelfinance.account.kafka.producer;

import com.emmanuelfinance.shared.kafka.account.AccountEventDTO;
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

    public void publishAccountCreate(AccountEventDTO event) {
        log.info("Publicando evento de conta criada no Kafka: {}", event.accountId());

        // envia evento para o tópico usando o ID da conta como chave
        kafkaTemplate.send(TOPIC, event.accountId().toString(), event);
    }
}
