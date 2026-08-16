package com.emmanuelfinance.creditcard.dto;

import com.emmanuelfinance.creditcard.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseCreditCardDTO(
        UUID id,
        AccountSummaryDTO account,
        String name,
        BanksEnum bank,
        BigDecimal creditLimit,
        Integer closingDay,
        Integer dueDay,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
