package com.emmanuelfinance.transaction.dtos;

import com.emmanuelfinance.shared.enums.TypeEnum;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTransactionDTO(
        @NotNull(message = "The account id is required")
        UUID accountId,

        UUID creditCardId,

        @NotNull(message = "The category id is required")
        UUID categoryId,

        String description,

        @NotNull(message = "The amount is required")
        BigDecimal amount,

        boolean scheduled,

        LocalDateTime date,

        @NotNull(message = "The type is required")
        TypeEnum type
) {}
