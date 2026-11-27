package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.shared.modules.account.dto.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.enums.StatusEventEnum;
import com.emmanuelfinance.shared.enums.FailedOperationEnum;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionFailedEvent;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferDeletedAndRestoreEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferFailedEvent;
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
class AccountProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AccountProducer accountProducer;

    private AccountEventDTO event(StatusEventEnum status) {
        return new AccountEventDTO(UUID.randomUUID(), UUID.randomUUID(), status);
    }

    private void confirmKafka() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    void shouldPublishTheCreatedEventInTheAccountEventsTopicUsingTheIdAsKey() {
        AccountEventDTO event = event(StatusEventEnum.CREATED);
        confirmKafka();

        accountProducer.publishAccountCreated(event);

        verify(kafkaTemplate).send("account-events", event.accountId().toString(), event);
    }

    @Test
    void shouldPublishTheDeletedEventInTheAccountEventsTopicUsingTheIdAsKey() {
        AccountEventDTO event = event(StatusEventEnum.DELETED);
        confirmKafka();

        accountProducer.publishAccountDeleted(event);

        verify(kafkaTemplate).send("account-events", event.accountId().toString(), event);
    }

    @Test
    void shouldPublishTheRestoredEventInTheAccountEventsTopicUsingTheIdAsKey() {
        AccountEventDTO event = event(StatusEventEnum.RESTORE);
        confirmKafka();

        accountProducer.publishAccountRestored(event);

        verify(kafkaTemplate).send("account-events", event.accountId().toString(), event);
    }

    @Test
    void shouldThrowWhenKafkaDoesNotConfirmSoTheOutboxKeepsTheRowPending() {
        AccountEventDTO event = event(StatusEventEnum.DELETED);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka fora do ar")));

        assertThrows(CompletionException.class, () -> accountProducer.publishAccountDeleted(event));
    }

    @Test
    void shouldPublishTheFailedTransaction() {
        TransactionCreatedEvent failed = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), BigDecimal.TEN, 1,
                TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID());
        confirmKafka();

        accountProducer.onFailure("transaction-created-topic", failed);

        verify(kafkaTemplate).send("transaction-failed-topic", failed.transactionId().toString(),
                TransactionFailedEvent.from("transaction-created-topic", failed));
    }

    @Test
    void shouldPublishTheFailedTransfer() {
        TransferDeletedAndRestoreEventDTO failed = new TransferDeletedAndRestoreEventDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, UUID.randomUUID());
        confirmKafka();

        accountProducer.onFailure("transfer-deleted-topic", failed);

        verify(kafkaTemplate).send("transfer-failed-topic", failed.transferId().toString(),
                new TransferFailedEvent(failed.transferId(), FailedOperationEnum.DELETE, null, null, null));
    }

    @Test
    void shouldIgnoreAFailedMessageThatIsNotAnOperation() {
        accountProducer.onFailure("account-events", "qualquer coisa");

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldThrowWhenKafkaDoesNotConfirmTheFailure() {
        TransferCreatedEventDTO failed = new TransferCreatedEventDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, UUID.randomUUID());
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("kafka fora do ar")));

        assertThrows(CompletionException.class,
                () -> accountProducer.onFailure("transfer-created-topic", failed));
    }
}
