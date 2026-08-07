package com.emmanuelfinance.creditcard.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCreditCardDTO (
        @NotNull(message = "The account id is required")
        UUID accountId,

        @NotNull(message = "The name is required")
        String name,

        BigDecimal creditLimit,

        @NotNull(message = "The closing day is required")
        Integer closingDay,

        @NotNull(message = "The due day is required")
        Integer dueDay
) {}