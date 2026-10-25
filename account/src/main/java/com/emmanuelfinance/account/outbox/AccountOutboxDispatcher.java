package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.kafka.AccountProducer;
import com.emmanuelfinance.account.outbox.dto.AccountOwnerDTO;
import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.account.query.AccountProjector;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountOutboxDispatcher implements OutboxDispatcher {

    private static final List<OutboxEventType> SUPPORTED_TYPES = List.of(
            AccountOutboxEventType.ACCOUNT_CREATED,
            AccountOutboxEventType.ACCOUNT_DELETED,
            AccountOutboxEventType.ACCOUNT_RESTORED,
            AccountOutboxEventType.ACCOUNT_CHANGED,
            AccountOutboxEventType.ACCOUNT_OWNER_CACHED,
            AccountOutboxEventType.ACCOUNT_OWNER_REMOVED
    );

    private final AccountProducer accountProducer;
    private final AccountProjector accountProjector;
    private final AccountCache accountCache;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(OutboxEventType type) {
        return SUPPORTED_TYPES.contains(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        switch (outboxEvent.getType()) {
            case AccountOutboxEventType.ACCOUNT_CREATED ->
                    accountProducer.publishAccountCreated(accountEvent(outboxEvent));
            case AccountOutboxEventType.ACCOUNT_DELETED ->
                    accountProducer.publishAccountDeleted(accountEvent(outboxEvent));
            case AccountOutboxEventType.ACCOUNT_RESTORED ->
                    accountProducer.publishAccountRestored(accountEvent(outboxEvent));
            case AccountOutboxEventType.ACCOUNT_CHANGED -> {
                AccountDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), AccountDocument.class);
                accountProjector.project(snapshot);
            }
            case AccountOutboxEventType.ACCOUNT_OWNER_CACHED -> {
                AccountOwnerDTO owner = objectMapper.treeToValue(outboxEvent.getPayload(), AccountOwnerDTO.class);
                accountCache.saveAccountOwner(owner.accountId(), owner.userId());

                log.info("Outbox: dono da conta {} gravado no cache (Redis)", owner.accountId());
            }
            case AccountOutboxEventType.ACCOUNT_OWNER_REMOVED -> {
                AccountOwnerDTO owner = objectMapper.treeToValue(outboxEvent.getPayload(), AccountOwnerDTO.class);
                accountCache.deleteAccountOwner(owner.accountId());

                log.info("Outbox: dono da conta {} removido do cache (Redis)", owner.accountId());
            }
            default -> throw new IllegalArgumentException("Tipo de evento não suportado por este dispatcher: " + outboxEvent.getType());
        }
    }

    private AccountEventDTO accountEvent(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        return objectMapper.treeToValue(outboxEvent.getPayload(), AccountEventDTO.class);
    }
}
