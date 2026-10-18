package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.outbox.dto.AccountOwnerDTO;
import com.emmanuelfinance.account.outbox.enums.OutboxEventType;
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

    public OutboxEvent accountEvent(Account account, StatusEventEnum status) {
        return create(OutboxEventType.ACCOUNT_EVENT, account.getId(),
                new AccountEventDTO(account.getId(), account.getUserId(), status));
    }

    public OutboxEvent accountSnapshot(Account account, boolean deleted) {
        AccountDocument snapshot = accountMapper.toDocument(account);
        snapshot.setDeleted(deleted);

        return create(OutboxEventType.ACCOUNT_CHANGED, account.getId(), snapshot);
    }

    public OutboxEvent accountOwner(Account account) {
        return create(OutboxEventType.ACCOUNT_OWNER_CACHED, account.getId(),
                new AccountOwnerDTO(account.getId(), account.getUserId()));
    }

    private OutboxEvent create(OutboxEventType type, UUID accountId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setType(type);
        outboxEvent.setAggregateId(accountId.toString());
        outboxEvent.setPayload(objectMapper.valueToTree(payload));
        outboxEvent.setCreatedAt(LocalDateTime.now());

        return outboxEvent;
    }
}
