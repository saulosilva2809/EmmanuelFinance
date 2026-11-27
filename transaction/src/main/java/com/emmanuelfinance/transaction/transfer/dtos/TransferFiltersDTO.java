package com.emmanuelfinance.transaction.transfer.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferFiltersDTO (
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amountMin,
        BigDecimal amountMax,
        LocalDateTime createdAt
) {}
