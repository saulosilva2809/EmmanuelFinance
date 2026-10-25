package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventFactory {

    private final ObjectMapper objectMapper;
    private final AccountMapper accountMapper;

    public OutboxEvent accountCreatedEvent(Account account) {
        return accountEvent(AccountOutboxEventType.ACCOUNT_CREATED, account, StatusEventEnum.CREATED);
    }

    public OutboxEvent accountDeletedEvent(Account account) {
        return accountEvent(AccountOutboxEventType.ACCOUNT_DELETED, account, StatusEventEnum.DELETED);
    }

    public OutboxEvent accountRestoredEvent(Account account) {
        return accountEvent(AccountOutboxEventType.ACCOUNT_RESTORED, account, StatusEventEnum.RESTORE);
    }

    public OutboxEvent accountSnapshot(Account account, boolean deleted) {
        AccountDocument snapshot = accountMapper.toDocument(account);
        snapshot.setDeleted(deleted);

        return create(AccountOutboxEventType.ACCOUNT_CHANGED, account.getId(), snapshot);
    }

    private OutboxEvent accountEvent(AccountOutboxEventType type, Account account, StatusEventEnum status) {
        return create(type, account.getId(), new AccountEventDTO(account.getId(), account.getUserId(), status));
    }

    private OutboxEvent create(AccountOutboxEventType type, UUID accountId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setType(type);
        outboxEvent.setAggregateId(accountId.toString());
        outboxEvent.setPayload(objectMapper.valueToTree(payload));
        outboxEvent.setCreatedAt(LocalDateTime.now());

        return outboxEvent;
    }
}
