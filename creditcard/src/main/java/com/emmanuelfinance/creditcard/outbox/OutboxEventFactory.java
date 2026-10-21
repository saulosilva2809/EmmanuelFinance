package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardMapper;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocument;
import com.emmanuelfinance.creditcard.outbox.enums.CreditCardOutboxEventType;
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
    private final CreditCardMapper creditCardMapper;

    public OutboxEvent createdInvoicePayment(InvoicePaymentDTO invoicePaymentDTO) {
        return create(CreditCardOutboxEventType.INVOICE_PAYMENT, invoicePaymentDTO.accountId(), invoicePaymentDTO);
    }

    public OutboxEvent creditCardSnapshot(CreditCard creditCard, boolean deleted) {
        CreditCardDocument snapshot = creditCardMapper.toDocument(creditCard);
        snapshot.setDeleted(deleted);

        return create(CreditCardOutboxEventType.CREDIT_CARD_CHANGED, creditCard.getId(), snapshot);
    }

    private OutboxEvent create(CreditCardOutboxEventType type, UUID aggregateId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setType(type);
        outboxEvent.setAggregateId(aggregateId.toString());
        outboxEvent.setPayload(objectMapper.valueToTree(payload));
        outboxEvent.setCreatedAt(LocalDateTime.now());

        return outboxEvent;
    }
}
