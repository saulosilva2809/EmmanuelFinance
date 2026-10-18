package com.emmanuelfinance.creditcard.payment.outbox;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventFactory {

    private final ObjectMapper objectMapper;

    public OutboxEvent createdInvoicePayment(InvoicePaymentDTO invoicePaymentDTO) {
        return create(PaymentOutboxEventType.INVOICE_PAYMENT, invoicePaymentDTO.accountId(), invoicePaymentDTO);
    }

    private OutboxEvent create(PaymentOutboxEventType type, UUID accountId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setType(type);
        outboxEvent.setAggregateId(accountId.toString());
        outboxEvent.setPayload(objectMapper.valueToTree(payload));
        outboxEvent.setCreatedAt(LocalDateTime.now());

        return outboxEvent;
    }
}
