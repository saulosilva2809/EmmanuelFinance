package com.emmanuelfinance.shared.kafka.account;

import com.emmanuelfinance.shared.kafka.account.enums.StatusEventEnum;
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
    private static final String REDIS_KEY_PREFIX = "account:";

    @KafkaListener(topics = "account-events", groupId = "shared-service-group")
    public void handleAccountEvent(AccountEventDTO event) {
        log.info("Evento de conta recebido no Category Service: {}", event);

        String redisKey = REDIS_KEY_PREFIX + event.accountId();

        if (StatusEventEnum.CREATED.equals(event.status())) {
            // salva a cache no redis com o valor true
            redisTemplate.opsForValue().set(redisKey, event.userId().toString());
            log.info("Conta {} armazenada no cache Redis com sucesso!", event.accountId());
        } else if (StatusEventEnum.DELETED.equals(event.status())) {
            redisTemplate.delete(redisKey);
            log.info("Conta {} removida do cache Redis", event.accountId());
        }
    }
}
