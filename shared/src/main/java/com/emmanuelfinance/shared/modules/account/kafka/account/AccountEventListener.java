package com.emmanuelfinance.shared.modules.account.kafka.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventListener {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String REDIS_KEY_PREFIX = "account:";

    @KafkaListener(topics = "account-events", groupId = "shared-service-group")
    public void handleAccountEvent(String eventJson) {
        try {
            AccountEventDTO event = objectMapper.readValue(eventJson, AccountEventDTO.class);
            log.info("Evento de conta recebido: {}", event);

            String redisKey = REDIS_KEY_PREFIX + event.accountId();

            if (StatusEventEnum.CREATED.equals(event.status())) {
                redisTemplate.opsForValue().set(redisKey, event.userId().toString());
                log.info("Conta {} armazenada no cache Redis com sucesso!", event.accountId());
            } else if (StatusEventEnum.DELETED.equals(event.status())) {
                redisTemplate.delete(redisKey);
                log.info("Conta {} removida do cache Redis", event.accountId());
            }
        } catch (Exception e) {
            log.error("Erro ao converter payload JSON para AccountEventDTO", e);
        }
    }
}