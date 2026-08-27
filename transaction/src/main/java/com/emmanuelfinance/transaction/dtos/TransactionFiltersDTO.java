package com.emmanuelfinance.transaction.dtos;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionFiltersDTO (
        UUID accountId,
        UUID categoryId,
        UUID creditCardId,
        BigDecimal greaterValueThan,
        BigDecimal valueLessThan,
        Boolean scheduled,
        LocalDate date,
        StatusTransactionEnum status,
        TypeEnum type
) {}