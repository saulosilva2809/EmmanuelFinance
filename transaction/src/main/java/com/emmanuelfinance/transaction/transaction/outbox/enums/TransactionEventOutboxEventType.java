package com.emmanuelfinance.transaction.transaction.outbox.enums;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;

public enum TransactionEventOutboxEventType implements OutboxEventType {

    TRANSACTION_CREATED,
    INVOICE_PAYMENT_CREATED,
    TRANSACTION_UPDATED,
    TRANSACTION_DELETED,
    TRANSACTION_RESTORED
}
