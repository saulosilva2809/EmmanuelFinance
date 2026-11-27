package com.emmanuelfinance.creditcard.payment.kafka;

import com.emmanuelfinance.creditcard.payment.services.PaymentService;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListener {

    private final PaymentService paymentService;

    // o transaction avisa por aqui que o pagamento da fatura não foi concluído
    @KafkaListener(topics = "invoice-payment-failed-topic", groupId = "credit-card-service-group")
    public void handleInvoicePaymentFailed(InvoicePaymentFailedEvent event) {
        log.warn("Pagamento da fatura {} falhou e será desfeito", event.invoiceId());

        paymentService.revertPayment(event);
    }
}
