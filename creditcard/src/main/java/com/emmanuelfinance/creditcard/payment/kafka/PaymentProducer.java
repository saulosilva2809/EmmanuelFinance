package com.emmanuelfinance.creditcard.payment.kafka;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CREATED_INVOICE_PAYMENT_TOPIC = "created-invoice-payment-topic";

    public void publishInvoicePaymentCreated(InvoicePaymentDTO event) {
        log.info("Publicando evento de pagamento de fatura, cartão de crédito: {}", event.creditCardId());
        kafkaTemplate.send(CREATED_INVOICE_PAYMENT_TOPIC, event);
    }
}