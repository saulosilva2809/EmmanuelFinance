package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.shared.modules.account.dto.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.enums.StatusEventEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

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
    private KafkaTemplate<String, AccountEventDTO> kafkaTemplate;

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
}
