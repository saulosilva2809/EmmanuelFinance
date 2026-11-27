package com.emmanuelfinance.shared.modules.transaction.transaction.dtos;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;

import java.math.BigDecimal;

public record TransactionSummaryDTO (
        CategorySummaryDTO category,
        BigDecimal amount,
        StatusTransactionEnum status,
        TypeTransactionEnum type
) {}
