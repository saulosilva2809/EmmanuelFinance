package com.emmanuelfinance.shared.modules.transaction.kafka.dto;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionUpdatedEvent(
        UUID transactionId,
        UUID oldAccountId,
        UUID newAccountId,
        UUID userId,
        BigDecimal oldAmount,
        BigDecimal newAmount,
        TypeEnum oldType,
        TypeEnum newType,
        StatusTransactionEnum oldStatus,
        StatusTransactionEnum newStatus
) {
}