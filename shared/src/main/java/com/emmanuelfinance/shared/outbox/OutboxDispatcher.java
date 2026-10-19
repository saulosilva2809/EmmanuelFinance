package com.emmanuelfinance.shared.outbox;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface OutboxDispatcher {

    boolean supports(OutboxEventType type);
    void dispatch(BaseOutboxEvent event) throws JsonProcessingException;
}
