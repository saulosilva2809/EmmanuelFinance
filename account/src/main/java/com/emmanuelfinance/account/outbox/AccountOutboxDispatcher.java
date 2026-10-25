package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.kafka.AccountProducer;
import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.account.query.AccountProjector;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountOutboxDispatcher implements OutboxDispatcher {

    private static final List<OutboxEventType> SUPPORTED_TYPES = List.of(
            AccountOutboxEventType.ACCOUNT_CREATED,
            AccountOutboxEventType.ACCOUNT_DELETED,
            AccountOutboxEventType.ACCOUNT_RESTORED,
            AccountOutboxEventType.ACCOUNT_CHANGED
    );

    private final AccountProducer accountProducer;
    private final AccountProjector accountProjector;
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
            default -> throw new IllegalArgumentException("Tipo de evento não suportado por este dispatcher: " + outboxEvent.getType());
        }
    }

    private AccountEventDTO accountEvent(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        return objectMapper.treeToValue(outboxEvent.getPayload(), AccountEventDTO.class);
    }
}
