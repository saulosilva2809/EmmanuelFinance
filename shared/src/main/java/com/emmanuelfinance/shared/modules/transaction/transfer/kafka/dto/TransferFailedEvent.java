package com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto;

import com.emmanuelfinance.shared.enums.FailedOperationEnum;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferFailedEvent(
        UUID transferId,
        FailedOperationEnum operation,
        UUID oldSourceAccountId,
        UUID oldDestinationAccountId,
        BigDecimal oldAmount
) {

    private static final String DELETED_TOPIC = "transfer-deleted-topic";

    public static TransferFailedEvent from(String topic, TransferEvent event) {
        if (event instanceof TransferUpdatedEventDTO updated) {
            return new TransferFailedEvent(updated.transferId(), FailedOperationEnum.UPDATE,
                    updated.oldSourceAccountId(), updated.oldDestinationAccountId(), updated.oldAmount());
        }

        FailedOperationEnum operation = FailedOperationEnum.CREATE;
        if (event instanceof TransferDeletedAndRestoreEventDTO) {
            operation = DELETED_TOPIC.equals(topic) ? FailedOperationEnum.DELETE : FailedOperationEnum.RESTORE;
        }

        return new TransferFailedEvent(event.transferId(), operation, null, null, null);
    }
}
