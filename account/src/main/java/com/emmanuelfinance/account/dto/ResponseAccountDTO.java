package com.emmanuelfinance.account.dto;

import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseAccountDTO (
        UUID id,
        String name,
        TypeEnum type,
        BanksEnum bank,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {}