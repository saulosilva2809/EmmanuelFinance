package com.emmanuelfinance.creditcard.creditcard.kafka;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionFailedEvent;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CreditCardProducer creditCardProducer;

    private final TransactionCreatedEvent failed = new TransactionCreatedEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, 1,
            TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID());

    @Test
    void shouldPublishTheFailedTransaction() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        creditCardProducer.onFailure("transaction-created-topic", failed);

        verify(kafkaTemplate).send("transaction-failed-topic", failed.transactionId().toString(),
                TransactionFailedEvent.from("transaction-created-topic", failed));
    }

    @Test
    void shouldThrowWhenKafkaDoesNotConfirmTheFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka fora do ar")));

        assertThrows(CompletionException.class,
                () -> creditCardProducer.onFailure("transaction-created-topic", failed));
    }

    @Test
    void shouldIgnoreAFailedMessageThatIsNotATransaction() {
        creditCardProducer.onFailure("account-events", "qualquer coisa");

        verifyNoInteractions(kafkaTemplate);
    }
}
