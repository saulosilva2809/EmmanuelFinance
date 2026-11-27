package com.emmanuelfinance.transaction.transfer.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferDTO (
        @NotNull(message = "The sourceAccountId is required.")
        UUID sourceAccountId,

        @NotNull(message = "The destinationAccountId is required.")
        UUID destinationAccountId,

        @NotNull(message = "The amount is required.")
        @Positive(message = "The amount must be greater than zero.")
        BigDecimal amount,

        String description
) {}