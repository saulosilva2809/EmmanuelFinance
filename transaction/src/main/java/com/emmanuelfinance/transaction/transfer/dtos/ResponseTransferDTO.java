package com.emmanuelfinance.transaction.transfer.dtos;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseTransferDTO(
        UUID id,
        AccountSummaryDTO sourceAccount,
        AccountSummaryDTO destinationAccount,
        String description,
        BigDecimal amount,
        StatusTransactionEnum status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
}
