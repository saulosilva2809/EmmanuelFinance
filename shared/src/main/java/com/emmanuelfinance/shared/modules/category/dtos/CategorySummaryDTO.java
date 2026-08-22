package com.emmanuelfinance.shared.modules.category.dtos;

import java.util.UUID;

public record CategorySummaryDTO(
        UUID id,
        String name,
        boolean deleted
) {}
