package com.emmanuelfinance.creditcard.payment.kafka;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CREATED_INVOICE_PAYMENT_TOPIC = "created-invoice-payment-topic";
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    public void publishInvoicePaymentCreated(InvoicePaymentDTO event) {
        log.info("Publicando evento de pagamento de fatura, cartão de crédito: {}", event.creditCardId());
        kafkaTemplate.send(CREATED_INVOICE_PAYMENT_TOPIC, event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }
}
