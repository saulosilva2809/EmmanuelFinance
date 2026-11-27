package com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionUpdatedEvent(
        UUID transactionId,
        UUID oldAccountId,
        UUID newAccountId,
        UUID userId,
        BigDecimal oldAmount,
        BigDecimal newAmount,
        TypeTransactionEnum oldType,
        TypeTransactionEnum newType,
        StatusTransactionEnum oldStatus,
        StatusTransactionEnum newStatus,
        UUID eventId
) implements TransactionEvent {}