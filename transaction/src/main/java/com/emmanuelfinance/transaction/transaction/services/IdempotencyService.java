package com.emmanuelfinance.transaction.transaction.services;

import com.emmanuelfinance.transaction.transaction.dtos.IdempotencyDataDTO;
import com.emmanuelfinance.transaction.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.transaction.exceptions.TransactionErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(2);

    private String sha256(String rawData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar chave de idempotência", e);
        }
    }

    // monta a chave só com os dados que vierem preenchidos, então cada lugar passa o que tem. cada dado entra com o
    // nome e o tamanho, assim a conta de origem e a de destino com o mesmo id, ou um dado a menos, nunca viram o mesmo texto
    public String generateKey(IdempotencyDataDTO data) {
        StringBuilder rawData = new StringBuilder();

        appendIfPresent(rawData, "userId", data.userId());
        appendIfPresent(rawData, "accountId", data.accountId());
        appendIfPresent(rawData, "destinationAccountId", data.destinationAccountId());
        appendIfPresent(rawData, "creditCardId", data.creditCardId());
        appendIfPresent(rawData, "categoryId", data.categoryId());
        appendIfPresent(rawData, "referenceId", data.referenceId());
        // 100 e 100.00 são o mesmo valor, então a escala não pode mudar a chave
        appendIfPresent(rawData, "amount", data.amount() != null ? data.amount().stripTrailingZeros().toPlainString() : null);
        appendIfPresent(rawData, "date", data.date());
        // caixa e espaços nas pontas não mudam a descrição
        appendIfPresent(rawData, "description", data.description() != null ? data.description().trim().toLowerCase(Locale.ROOT) : null);

        // sem nenhum dado todas as chamadas gerariam a mesma chave e travariam umas às outras
        if (rawData.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um dado para gerar a chave de idempotência.");
        }

        return sha256(rawData.toString());
    }

    private void appendIfPresent(StringBuilder rawData, String name, Object value) {
        if (value == null || value.toString().isEmpty()) {
            return;
        }

        String text = value.toString();
        rawData.append(name).append('=').append(text.length()).append(':').append(text).append(';');
    }

    public void validateAndLock(String idempotencyKey) {
        String key = "idempotency:transaction:" + idempotencyKey;

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(key, "LOCKED", IDEMPOTENCY_TTL);

        if (!Boolean.TRUE.equals(isFirstRequest)) {
            throw new TransactionDomainException(TransactionErrorCode.TRANSACTION_ALREADY_EXISTS);
        }

        releaseIfNotCommitted(key);
    }

    // se a operação não for gravada, a trava sai na hora para o usuário poder tentar de novo
    private void releaseIfNotCommitted(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    redisTemplate.delete(key);
                }
            }
        });
    }
}