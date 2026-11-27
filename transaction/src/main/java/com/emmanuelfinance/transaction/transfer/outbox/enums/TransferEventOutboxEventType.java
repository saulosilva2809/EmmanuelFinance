package com.emmanuelfinance.transaction.transfer.outbox.enums;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;

public enum TransferEventOutboxEventType implements OutboxEventType {

    TRANSFER_CREATED,
    TRANSFER_UPDATED,
    TRANSFER_DELETED,
    TRANSFER_RESTORED
}
