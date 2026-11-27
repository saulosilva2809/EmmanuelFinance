package com.emmanuelfinance.transaction.transfer;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentFailedEvent;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import com.emmanuelfinance.transaction.transaction.kafka.TransactionProducer;
import com.emmanuelfinance.transaction.transfer.dtos.TransferFiltersDTO;
import com.emmanuelfinance.transaction.transfer.kafka.TransferProducer;
import com.emmanuelfinance.transaction.transfer.query.TransferSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferDeliveryTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final TransferCreatedEventDTO event = new TransferCreatedEventDTO(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, UUID.randomUUID());

    private void confirmKafka() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    void shouldPublishTheTransferUsingItsIdAsKey() {
        confirmKafka();

        new TransferProducer(kafkaTemplate).publishTransferCreated(event);

        verify(kafkaTemplate).send("transfer-created-topic", event.transferId().toString(), event);
    }

    @Test
    void shouldThrowWhenKafkaDoesNotConfirmSoTheOutboxKeepsTheRow() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka fora do ar")));

        assertThrows(CompletionException.class,
                () -> new TransferProducer(kafkaTemplate).publishTransferCreated(event));
    }

    @Test
    void shouldAskTheCardToRevertAnInvoicePaymentThatCouldNotBeRecorded() {
        InvoicePaymentDTO payment = new InvoicePaymentDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Cartão", UUID.randomUUID(), 10, 2026, BigDecimal.TEN, UUID.randomUUID());
        confirmKafka();

        new TransactionProducer(kafkaTemplate).onFailure("created-invoice-payment-topic", payment);

        verify(kafkaTemplate).send("invoice-payment-failed-topic", payment.invoiceId().toString(),
                new InvoicePaymentFailedEvent(
                        payment.paymentId(), payment.creditCardId(), payment.invoiceId(), payment.totalPaid()));
    }

    @Test
    void shouldFilterByTheWholeDay() {
        String query = TransferSpecification.withFilter(new TransferFiltersDTO(
                null, null, null, null, LocalDateTime.of(2026, 10, 5, 15, 30)), UUID.randomUUID(), false)
                .toString();

        assertTrue(query.contains("createdAt"));
    }
}
