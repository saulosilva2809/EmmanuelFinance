package com.emmanuelfinance.creditcard.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentPartialDTO(

        @NotNull(message = "The accountId is required.")
        UUID accountId,

        @NotNull(message = "The amount is required.")
        @Positive(message = "The amount must be greater than zero.")
        BigDecimal amount
) {}
