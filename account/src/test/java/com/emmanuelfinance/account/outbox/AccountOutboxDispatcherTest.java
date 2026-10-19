package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.outbox.dto.AccountOwnerDTO;
import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.account.query.AccountProjector;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountOutboxDispatcherTest {

    @Mock
    private KafkaTemplate<String, AccountEventDTO> kafkaTemplate;

    @Mock
    private AccountProjector accountProjector;

    @Mock
    private AccountCache accountCache;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private AccountOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void shouldSupportOnlyTheAccountTypes() {
        assertTrue(dispatcher.supports(AccountOutboxEventType.ACCOUNT_EVENT));
        assertTrue(dispatcher.supports(AccountOutboxEventType.ACCOUNT_CHANGED));
        assertTrue(dispatcher.supports(AccountOutboxEventType.ACCOUNT_OWNER_CACHED));
        assertFalse(dispatcher.supports(new OutboxEventType() {}));
    }

    @Test
    void shouldSendTheShortEventToKafka() throws Exception {
        AccountEventDTO event = new AccountEventDTO(UUID.randomUUID(), UUID.randomUUID(), StatusEventEnum.CREATED);

        dispatcher.dispatch(row(AccountOutboxEventType.ACCOUNT_EVENT, event.accountId(), event));

        verify(kafkaTemplate).send("account-events", event.accountId().toString(), event);
        verifyNoInteractions(accountProjector, accountCache);
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
        verifyNoInteractions(kafkaTemplate, accountCache);
    }

    @Test
    void shouldSaveTheOwnerInTheCache() throws Exception {
        AccountOwnerDTO owner = new AccountOwnerDTO(UUID.randomUUID(), UUID.randomUUID());

        dispatcher.dispatch(row(AccountOutboxEventType.ACCOUNT_OWNER_CACHED, owner.accountId(), owner));

        verify(accountCache).saveAccountOwner(owner.accountId(), owner.userId());
        verifyNoInteractions(kafkaTemplate, accountProjector);
    }

    @Test
    void shouldLetTheExceptionOfTheDestinationGoUpSoTheRowStaysPending() {
        AccountOwnerDTO owner = new AccountOwnerDTO(UUID.randomUUID(), UUID.randomUUID());
        doThrow(new IllegalStateException("redis fora do ar")).when(accountCache).saveAccountOwner(any(), any());

        assertThrows(IllegalStateException.class, () ->
                dispatcher.dispatch(row(AccountOutboxEventType.ACCOUNT_OWNER_CACHED, owner.accountId(), owner)));
    }

    private OutboxEvent row(AccountOutboxEventType type, UUID accountId, Object payload) {
        OutboxEvent row = new OutboxEvent();
        row.setType(type);
        row.setAggregateId(accountId.toString());
        row.setPayload(objectMapper.valueToTree(payload));
        return row;
    }
}
