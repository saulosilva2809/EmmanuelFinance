package com.emmanuelfinance.transaction.kafka;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionProducer transactionProducer;

    private TransactionCreatedEvent event() {
        return new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), 1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now()
        );
    }

    private TransactionDeletedAndRestoreEvent deletedEvent() {
        return new TransactionDeletedAndRestoreEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), 1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now()
        );
    }

    private TransactionUpdatedEvent updatedEvent() {
        return new TransactionUpdatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), new BigDecimal("150.00"), TypeEnum.EXPENSE, TypeEnum.EXPENSE,
                StatusTransactionEnum.PAID, StatusTransactionEnum.PAID
        );
    }

    private void confirmKafka() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    @DisplayName("Deve publicar a transação criada em transaction-created-topic usando o id como chave")
    void shouldPublishTransactionCreated() {
        TransactionCreatedEvent event = event();
        confirmKafka();

        transactionProducer.publishTransactionCreated(event);

        verify(kafkaTemplate, times(1)).send("transaction-created-topic", event.transactionId().toString(), event);
    }

    @Test
    @DisplayName("Deve publicar o pagamento de fatura em invoice-payment-created-topic usando o id como chave")
    void shouldPublishInvoicePaymentCreated() {
        TransactionCreatedEvent event = event();
        confirmKafka();

        transactionProducer.publishInvoicePaymentCreated(event);

        verify(kafkaTemplate, times(1))
                .send("invoice-payment-created-topic", event.transactionId().toString(), event);
    }

    @Test
    @DisplayName("Deve publicar a transação atualizada em transaction-updated-topic")
    void shouldPublishTransactionUpdated() {
        TransactionUpdatedEvent event = updatedEvent();
        confirmKafka();

        transactionProducer.publishTransactionUpdated(event);

        verify(kafkaTemplate, times(1)).send("transaction-updated-topic", event.transactionId().toString(), event);
    }

    @Test
    @DisplayName("Deve publicar a transação excluída em transaction-deleted-topic")
    void shouldPublishTransactionDeleted() {
        TransactionDeletedAndRestoreEvent event = deletedEvent();
        confirmKafka();

        transactionProducer.publishTransactionDeleted(event);

        verify(kafkaTemplate, times(1)).send("transaction-deleted-topic", event.transactionId().toString(), event);
    }

    @Test
    @DisplayName("Deve publicar a transação restaurada em transaction-restore-topic")
    void shouldPublishTransactionRestore() {
        TransactionDeletedAndRestoreEvent event = deletedEvent();
        confirmKafka();

        transactionProducer.publishTransactionRestore(event);

        verify(kafkaTemplate, times(1)).send("transaction-restore-topic", event.transactionId().toString(), event);
    }

    @Test
    @DisplayName("Deve lançar erro quando o Kafka não confirmar, para o Outbox manter a linha pendente")
    void shouldThrowWhenKafkaDoesNotConfirm() {
        TransactionCreatedEvent event = event();
        when(kafkaTemplate.send(eq("transaction-created-topic"), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka fora do ar")));

        assertThrows(CompletionException.class, () -> transactionProducer.publishTransactionCreated(event));
    }
}
