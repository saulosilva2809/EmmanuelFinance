package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.exceptions.TransactionAlreadyExists;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(2);

    public String generateIdempotencyKey(UUID userId, CreateTransactionDTO data) {
        String rawData = String.format(
                "%s:%s:%s:%s:%s",
                userId,
                data.accountId(),
                data.amount(),
                data.date() != null ? data.date().toString() : "",
                data.description() != null ? data.description().trim().toLowerCase() : ""
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar chave de idempotência", e);
        }
    }

    public void validateAndLock(String idempotencyKey) {
        String key = "idempotency:transaction:" + idempotencyKey;

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", IDEMPOTENCY_TTL);

        if (!Boolean.TRUE.equals(isFirstRequest)) {
            throw new TransactionAlreadyExists();
        }
    }
}