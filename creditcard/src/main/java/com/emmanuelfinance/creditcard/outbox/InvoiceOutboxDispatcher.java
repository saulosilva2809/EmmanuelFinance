package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceProjector;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceOutboxEventType;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final InvoiceProjector invoiceProjector;

    @Override
    public boolean supports(OutboxEventType type) {
        return InvoiceOutboxEventType.INVOICE_CHANGED.is(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        InvoiceDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), InvoiceDocument.class);
        invoiceProjector.project(snapshot);
    }
}
