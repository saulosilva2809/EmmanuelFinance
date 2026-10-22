package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemProjector;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceItemOutboxEventType;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceItemOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final InvoiceItemProjector invoiceItemProjector;

    @Override
    public boolean supports(OutboxEventType type) {
        return InvoiceItemOutboxEventType.INVOICE_ITEM_CHANGED.is(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        InvoiceItemDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), InvoiceItemDocument.class);
        invoiceItemProjector.project(snapshot);
    }
}
