package com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto;

import com.emmanuelfinance.shared.enums.FailedOperationEnum;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionFailedEvent(
        UUID transactionId,
        FailedOperationEnum operation,
        UUID oldAccountId,
        BigDecimal oldAmount,
        TypeTransactionEnum oldType,
        StatusTransactionEnum oldStatus
) {

    private static final String DELETED_TOPIC = "transaction-deleted-topic";

    public static TransactionFailedEvent from(String topic, TransactionEvent event) {
        if (event instanceof TransactionUpdatedEvent updated) {
            return new TransactionFailedEvent(updated.transactionId(), FailedOperationEnum.UPDATE,
                    updated.oldAccountId(), updated.oldAmount(), updated.oldType(), updated.oldStatus());
        }

        FailedOperationEnum operation = FailedOperationEnum.CREATE;
        if (event instanceof TransactionDeletedAndRestoreEvent) {
            operation = DELETED_TOPIC.equals(topic) ? FailedOperationEnum.DELETE : FailedOperationEnum.RESTORE;
        }

        return new TransactionFailedEvent(event.transactionId(), operation, null, null, null, null);
    }
}
