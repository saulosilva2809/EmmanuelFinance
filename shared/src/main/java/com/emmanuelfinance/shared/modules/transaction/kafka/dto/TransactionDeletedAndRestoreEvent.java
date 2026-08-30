package com.emmanuelfinance.shared.modules.transaction.kafka.dto;

import com.emmanuelfinance.shared.enums.TypeEnum;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionDeletedAndRestoreEvent (
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        TypeEnum type
) {}
