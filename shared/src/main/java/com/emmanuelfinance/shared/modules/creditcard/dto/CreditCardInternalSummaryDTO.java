package com.emmanuelfinance.shared.modules.creditcard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditCardInternalSummaryDTO(
        UUID id,
        UUID accountId,
        BigDecimal availableLimit,
        Integer dueDate,
        Integer closingDate
) {}