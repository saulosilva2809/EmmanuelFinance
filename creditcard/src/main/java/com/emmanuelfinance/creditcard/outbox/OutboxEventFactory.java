package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardMapper;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocument;
import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.InvoiceMapper;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocument;
import com.emmanuelfinance.creditcard.outbox.enums.CreditCardOutboxEventType;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceItemOutboxEventType;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceOutboxEventType;
import com.emmanuelfinance.creditcard.outbox.enums.PaymentOutboxEventType;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
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
    private final InvoiceMapper invoiceMapper;

    public OutboxEvent createdInvoicePayment(InvoicePaymentDTO invoicePaymentDTO) {
        return create(PaymentOutboxEventType.INVOICE_PAYMENT, invoicePaymentDTO.accountId(), invoicePaymentDTO);
    }

    public OutboxEvent creditCardSnapshot(CreditCard creditCard, boolean deleted) {
        CreditCardDocument snapshot = creditCardMapper.toDocument(creditCard);
        snapshot.setDeleted(deleted);

        return create(CreditCardOutboxEventType.CREDIT_CARD_CHANGED, creditCard.getId(), snapshot);
    }

    public OutboxEvent invoiceSnapshot(Invoice invoice, boolean deleted) {
        InvoiceDocument snapshot = invoiceMapper.toDocument(invoice);
        snapshot.setDeleted(deleted);

        return create(InvoiceOutboxEventType.INVOICE_CHANGED, invoice.getId(), snapshot);
    }

    public OutboxEvent invoiceItemSnapshot(InvoiceItem invoiceItem, boolean deleted) {
        InvoiceItemDocument snapshot = invoiceMapper.toItemDocument(invoiceItem);
        snapshot.setDeleted(deleted);

        return create(InvoiceItemOutboxEventType.INVOICE_ITEM_CHANGED, invoiceItem.getId(), snapshot);
    }

    private OutboxEvent create(OutboxEventType type, UUID aggregateId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setType(type);
        outboxEvent.setAggregateId(aggregateId.toString());
        outboxEvent.setPayload(objectMapper.valueToTree(payload));
        outboxEvent.setCreatedAt(LocalDateTime.now());

        return outboxEvent;
    }
}
