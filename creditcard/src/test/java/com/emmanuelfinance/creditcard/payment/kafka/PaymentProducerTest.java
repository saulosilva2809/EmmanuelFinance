package com.emmanuelfinance.creditcard.payment.kafka;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentProducer paymentProducer;

    private InvoicePaymentDTO newEvent() {
        return new InvoicePaymentDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Cartão C6",
                UUID.randomUUID(), 10, 2026, new BigDecimal("300.00")
        );
    }

    @Test
    @DisplayName("Deve enviar o evento de pagamento para o tópico created-invoice-payment-topic")
    void shouldSendPaymentEventToTheTopic() {
        InvoicePaymentDTO event = newEvent();
        when(kafkaTemplate.send("created-invoice-payment-topic", event))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        paymentProducer.publishInvoicePaymentCreated(event);

        verify(kafkaTemplate, times(1)).send("created-invoice-payment-topic", event);
    }

    @Test
    @DisplayName("Deve lançar erro quando o Kafka não confirmar, para o Outbox manter a linha pendente")
    void shouldThrowWhenKafkaDoesNotConfirm() {
        InvoicePaymentDTO event = newEvent();
        when(kafkaTemplate.send("created-invoice-payment-topic", event))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka fora do ar")));

        assertThrows(CompletionException.class, () -> paymentProducer.publishInvoicePaymentCreated(event));
    }
}
