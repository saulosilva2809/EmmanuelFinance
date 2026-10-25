package com.emmanuelfinance.account.outbox.enums;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;

public enum AccountOutboxEventType implements OutboxEventType {

    ACCOUNT_CREATED,
    ACCOUNT_DELETED,
    ACCOUNT_RESTORED,
    ACCOUNT_CHANGED,
    ACCOUNT_OWNER_CACHED,
    ACCOUNT_OWNER_REMOVED
}
