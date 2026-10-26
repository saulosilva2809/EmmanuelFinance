package com.emmanuelfinance.shared.modules.category.dtos;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;

import java.util.UUID;

public record CategoryInternalSummaryDTO(
        UUID id,
        TypeTransactionEnum type
) {
}
