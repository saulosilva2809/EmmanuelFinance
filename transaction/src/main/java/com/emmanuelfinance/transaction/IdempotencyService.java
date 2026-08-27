package com.emmanuelfinance.transaction;

import com.emmanuelfinance.transaction.exceptions.TransactionAlreadyExists;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(2);

    public void validateAndLock(String idempotencyKey) {
        String key = "idempotency:transaction:" + idempotencyKey;

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", IDEMPOTENCY_TTL);

        if (!Boolean.TRUE.equals(isFirstRequest)) {
            throw new TransactionAlreadyExists();
        }
    }
}