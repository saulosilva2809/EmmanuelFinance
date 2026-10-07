package com.emmanuelfinance.creditcard.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentTotalDTO (

        @NotNull(message = "The accountId is required.")
        UUID accountId
) {}
