package com.emmanuelfinance.account.dto;

import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseAccountDTO (
        UUID id,
        UserSummaryDTO user,
        String name,
        TypeEnum type,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}