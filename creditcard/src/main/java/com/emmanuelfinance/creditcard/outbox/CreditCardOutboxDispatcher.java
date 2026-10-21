package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocument;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardProjector;
import com.emmanuelfinance.creditcard.outbox.enums.CreditCardOutboxEventType;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreditCardOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final CreditCardProjector creditCardProjector;

    @Override
    public boolean supports(OutboxEventType type) {
        return CreditCardOutboxEventType.CREDIT_CARD_CHANGED.equals(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        CreditCardDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), CreditCardDocument.class);
        creditCardProjector.project(snapshot);
    }
}
