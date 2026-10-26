package com.emmanuelfinance.transaction.transaction.dtos;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTransactionDTO(
        UUID accountId,
        UUID categoryId,
        String description,
        BigDecimal amount,
        Boolean scheduled,
        LocalDateTime date,
        TypeTransactionEnum type
) {}