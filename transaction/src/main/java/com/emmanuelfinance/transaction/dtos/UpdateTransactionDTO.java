package com.emmanuelfinance.transaction.dtos;

import com.emmanuelfinance.shared.enums.TypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTransactionDTO(
        UUID accountId,
        UUID categoryId,
        UUID creditCardId,
        String description,
        BigDecimal amount,
        Boolean scheduled,
        LocalDateTime date,
        TypeEnum type
) {}