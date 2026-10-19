package com.emmanuelfinance.account.outbox.enums;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;

public enum AccountOutboxEventType implements OutboxEventType {

    ACCOUNT_EVENT,
    ACCOUNT_CHANGED,
    ACCOUNT_OWNER_CACHED
}
