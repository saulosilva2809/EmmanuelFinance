package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.exceptions.TransactionErrorCode;
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

        return sha256(rawData);
    }

    /**
     * Chave estável por fatura: a reentrega do evento de pagamento gera sempre a mesma chave.
     */
    public String generateInvoicePaymentKey(UUID userId, UUID invoiceId) {
        return sha256(String.format("invoice-payment:%s:%s", userId, invoiceId));
    }

    private String sha256(String rawData) {
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
            throw new TransactionDomainException(TransactionErrorCode.TRANSACTION_ALREADY_EXISTS);
        }
    }
}