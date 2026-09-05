package com.emmanuelfinance.transaction.dtos;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseTransactionDTO(
        UUID id,
        AccountSummaryDTO accountSummary,
        CreditCardSummaryDTO creditCardSummary,
        CategorySummaryDTO categorySummary,
        UUID recurringId,
        String description,
        BigDecimal amount,
        Integer installmentsCount,
        boolean scheduled,
        LocalDateTime date,
        StatusTransactionEnum status,
        TypeEnum type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {}