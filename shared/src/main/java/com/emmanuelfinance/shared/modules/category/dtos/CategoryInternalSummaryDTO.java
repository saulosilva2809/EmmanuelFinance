package com.emmanuelfinance.shared.modules.category.dtos;

import com.emmanuelfinance.shared.enums.TypeEnum;

import java.util.UUID;

public record CategoryInternalSummaryDTO(
        UUID id,
        TypeEnum type
) {
}
