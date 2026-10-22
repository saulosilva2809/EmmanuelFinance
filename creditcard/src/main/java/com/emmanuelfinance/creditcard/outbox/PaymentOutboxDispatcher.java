package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.outbox.enums.PaymentOutboxEventType;
import com.emmanuelfinance.creditcard.payment.kafka.PaymentProducer;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final PaymentProducer paymentProducer;

    @Override
    public boolean supports(com.emmanuelfinance.shared.outbox.enums.OutboxEventType type) {
        return PaymentOutboxEventType.INVOICE_PAYMENT.is(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent event) throws JsonProcessingException {
        InvoicePaymentDTO invoicePaymentDTO = objectMapper.treeToValue(event.getPayload(), InvoicePaymentDTO.class);

        paymentProducer.publishInvoicePaymentCreated(new InvoicePaymentDTO(
                invoicePaymentDTO.accountId(),
                invoicePaymentDTO.userId(),
                invoicePaymentDTO.creditCardId(),
                invoicePaymentDTO.creditCardName(),
                invoicePaymentDTO.invoiceId(),
                invoicePaymentDTO.month(),
                invoicePaymentDTO.year(),
                invoicePaymentDTO.totalPaid()
        ));
    }
}
