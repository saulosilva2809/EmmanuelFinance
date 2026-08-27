package com.emmanuelfinance.shared.modules.creditcard.dto;

import java.util.UUID;

public record CreditCardInternalSummaryDTO(
        UUID id,
        UUID accountId
) {}