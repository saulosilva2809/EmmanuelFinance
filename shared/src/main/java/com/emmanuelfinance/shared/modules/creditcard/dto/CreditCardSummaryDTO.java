package com.emmanuelfinance.shared.modules.creditcard.dto;

import java.util.UUID;

public record CreditCardSummaryDTO (
        UUID id,
        String name,
        boolean deleted
) {}