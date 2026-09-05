package com.emmanuelfinance.shared.modules.transaction.dtos;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;

import java.math.BigDecimal;

public record TransactionSummaryDTO (
        CategorySummaryDTO category,
        BigDecimal amount,
        StatusTransactionEnum status,
        TypeEnum type
) {}
