package com.emmanuelfinance.transaction.transaction.dtos;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record IdempotencyDataDTO(
        UUID userId,
        UUID accountId,
        UUID destinationAccountId,
        UUID creditCardId,
        UUID categoryId,
        UUID referenceId,
        BigDecimal amount,
        LocalDateTime date,
        String description
) {}
