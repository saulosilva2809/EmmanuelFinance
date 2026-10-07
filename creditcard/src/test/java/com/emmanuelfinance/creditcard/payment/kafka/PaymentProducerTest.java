package com.emmanuelfinance.creditcard.payment.kafka;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentProducer paymentProducer;

    @Test
    @DisplayName("Deve enviar o evento de pagamento para o tópico created-invoice-payment-topic")
    void shouldSendPaymentEventToTheTopic() {
        InvoicePaymentDTO event = new InvoicePaymentDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Cartão C6",
                UUID.randomUUID(), 10, 2026, new BigDecimal("300.00")
        );

        paymentProducer.publishInvoicePaymentCreated(event);

        verify(kafkaTemplate, times(1)).send("created-invoice-payment-topic", event);
    }
}
