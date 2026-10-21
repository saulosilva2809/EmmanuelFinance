package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.outbox.dto.AccountOwnerDTO;
import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.account.query.AccountProjector;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountOutboxDispatcher implements OutboxDispatcher {

    private static final String ACCOUNT_EVENTS_TOPIC = "account-events";
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    private static final List<OutboxEventType> SUPPORTED_TYPES = List.of(
            AccountOutboxEventType.ACCOUNT_EVENT,
            AccountOutboxEventType.ACCOUNT_CHANGED,
            AccountOutboxEventType.ACCOUNT_OWNER_CACHED
    );

    private final KafkaTemplate<String, AccountEventDTO> kafkaTemplate;
    private final AccountProjector accountProjector;
    private final AccountCache accountCache;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(OutboxEventType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        switch (outboxEvent.getType()) {
            case AccountOutboxEventType.ACCOUNT_EVENT -> {
                AccountEventDTO payload = objectMapper.treeToValue(outboxEvent.getPayload(), AccountEventDTO.class);

                kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, outboxEvent.getAggregateId(), payload)
                        .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .join();

                log.info("Outbox: aviso {} da conta {} confirmado pelo Kafka (tópico {})",
                        payload.status(), outboxEvent.getAggregateId(), ACCOUNT_EVENTS_TOPIC);
            }
            case AccountOutboxEventType.ACCOUNT_CHANGED -> {
                AccountDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), AccountDocument.class);
                accountProjector.project(snapshot);
            }
            case AccountOutboxEventType.ACCOUNT_OWNER_CACHED -> {
                AccountOwnerDTO owner = objectMapper.treeToValue(outboxEvent.getPayload(), AccountOwnerDTO.class);
                accountCache.saveAccountOwner(owner.accountId(), owner.userId());

                log.info("Outbox: dono da conta {} gravado no cache (Redis)", owner.accountId());
            }
            default -> throw new IllegalArgumentException("Tipo de evento não suportado por este dispatcher: " + outboxEvent.getType());
        }
    }
}