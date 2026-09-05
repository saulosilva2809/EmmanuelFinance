package com.emmanuelfinance.shared.modules.transaction.kafka.dto;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID transactionId,
        UUID accountId,
        UUID creditCardId,
        UUID userId,
        BigDecimal amount,
        Integer installmentsCount,
        TypeEnum type,
        StatusTransactionEnum status,
        LocalDateTime date
) {}