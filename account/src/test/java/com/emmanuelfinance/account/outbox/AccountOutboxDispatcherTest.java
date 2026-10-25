package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.kafka.AccountProducer;
import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.account.query.AccountProjector;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountOutboxDispatcherTest {

    @Mock
    private AccountProducer accountProducer;

    @Mock
    private AccountProjector accountProjector;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private AccountOutboxDispatcher dispatcher;

    @Test
    void shouldSupportOnlyTheAccountTypes() {
        for (AccountOutboxEventType type : AccountOutboxEventType.values()) {
            assertTrue(dispatcher.supports(type), type.name());
        }
        assertFalse(dispatcher.supports(() -> "OUTRO_TIPO"));
    }

    @Test
    void shouldPublishTheCreatedEventThroughTheProducer() throws Exception {
        AccountEventDTO event = new AccountEventDTO(UUID.randomUUID(), UUID.randomUUID(), StatusEventEnum.CREATED);

        dispatcher.dispatch(row(AccountOutboxEventType.ACCOUNT_CREATED, event.accountId(), event));

        verify(accountProducer).publishAccountCreated(event);
        verifyNoMoreInteractions(accountProducer);
        verifyNoInteractions(accountProjector);
    }

    @Test
    void shouldPublishTheDeletedEventThroughTheProducer() throws Exception {
        AccountEventDTO event = new AccountEventDTO(UUID.randomUUID(), UUID.randomUUID(), StatusEventEnum.DELETED);

        dispatcher.dispatch(row(AccountOutboxEventType.ACCOUNT_DELETED, event.accountId(), event));

        verify(accountProducer).publishAccountDeleted(event);
        verifyNoMoreInteractions(accountProducer);
    }

    @Test
    void shouldPublishTheRestoredEventThroughTheProducer() throws Exception {
        AccountEventDTO event = new AccountEventDTO(UUID.randomUUID(), UUID.randomUUID(), StatusEventEnum.RESTORE);

        dispatcher.dispatch(row(AccountOutboxEventType.ACCOUNT_RESTORED, event.accountId(), event));

        verify(accountProducer).publishAccountRestored(event);
        verifyNoMoreInteractions(accountProducer);
    }

    @Test
    void shouldApplyTheSnapshotInMongo() throws Exception {
        AccountDocument snapshot = new AccountDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setName("Caixa");
        snapshot.setType(TypeEnum.CASH);
        snapshot.setCurrentBalance(new BigDecimal("50.00"));

        dispatcher.dispatch(row(AccountOutboxEventType.ACCOUNT_CHANGED, snapshot.getId(), snapshot));

        verify(accountProjector).project(argThat(received ->
                received.getId().equals(snapshot.getId())
                        && received.getCurrentBalance().equals(new BigDecimal("50.00"))));
        verifyNoInteractions(accountProducer);
    }

    @Test
    void shouldLetTheExceptionOfTheDestinationGoUpSoTheRowStaysPending() {
        AccountEventDTO event = new AccountEventDTO(UUID.randomUUID(), UUID.randomUUID(), StatusEventEnum.DELETED);
        doThrow(new IllegalStateException("kafka fora do ar")).when(accountProducer).publishAccountDeleted(any());

        assertThrows(IllegalStateException.class, () ->
                dispatcher.dispatch(row(AccountOutboxEventType.ACCOUNT_DELETED, event.accountId(), event)));
    }

    private OutboxEvent row(AccountOutboxEventType type, UUID accountId, Object payload) {
        OutboxEvent row = new OutboxEvent();
        row.setType(type);
        row.setAggregateId(accountId.toString());
        row.setPayload(objectMapper.valueToTree(payload));
        return row;
    }
}
