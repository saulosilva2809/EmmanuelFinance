package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.enums.TypeEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseCategoryDTO(
        UUID id,
        AccountSummaryDTO account,
        String name,
        TypeEnum type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {}
