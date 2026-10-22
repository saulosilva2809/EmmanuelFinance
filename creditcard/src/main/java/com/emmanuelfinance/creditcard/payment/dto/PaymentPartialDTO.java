package com.emmanuelfinance.creditcard.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentPartialDTO(

        @NotNull(message = "The accountId is required.")
        UUID accountId,

        @NotNull(message = "The amount is required.")
        BigDecimal amount
) {}
