package com.emmanuelfinance.shared.modules.transaction.kafka;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID transactionId,
        UUID accountId,
        BigDecimal amount,
        TypeEnum type,
        StatusTransactionEnum status
) {}