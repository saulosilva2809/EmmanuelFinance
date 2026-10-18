package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.outbox.dto.AccountOwnerDTO;
import com.emmanuelfinance.account.outbox.enums.OutboxEventType;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.account.query.AccountProjector;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, AccountEventDTO> kafkaTemplate;

    @Mock
    private AccountProjector accountProjector;

    @Mock
    private AccountCache accountCache;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private OutboxRelay outboxRelay;

    @Test
    void shouldSendTheShortEventToKafkaAndDeleteTheRow() {
        AccountEventDTO event = new AccountEventDTO(UUID.randomUUID(), UUID.randomUUID(), StatusEventEnum.CREATED);
        OutboxEvent row = row(OutboxEventType.ACCOUNT_EVENT, event.accountId(), event);
        whenPending(List.of(row));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        outboxRelay.drain();

        InOrder inOrder = inOrder(kafkaTemplate, outboxEventRepository);
        inOrder.verify(kafkaTemplate).send("account-events", event.accountId().toString(), event);
        inOrder.verify(outboxEventRepository).delete(row);
    }

    @Test
    void shouldApplyTheSnapshotInMongoAndDeleteTheRow() {
        AccountDocument snapshot = new AccountDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setName("Caixa");
        snapshot.setType(TypeEnum.CASH);
        snapshot.setCurrentBalance(new BigDecimal("50.00"));
        OutboxEvent row = row(OutboxEventType.ACCOUNT_CHANGED, snapshot.getId(), snapshot);
        whenPending(List.of(row));

        outboxRelay.drain();

        InOrder inOrder = inOrder(accountProjector, outboxEventRepository);
        inOrder.verify(accountProjector).project(argThat(received ->
                received.getId().equals(snapshot.getId())
                        && received.getCurrentBalance().equals(new BigDecimal("50.00"))));
        inOrder.verify(outboxEventRepository).delete(row);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void shouldSaveTheAccountOwnerInTheCacheAndDeleteTheRow() {
        AccountOwnerDTO owner = new AccountOwnerDTO(UUID.randomUUID(), UUID.randomUUID());
        OutboxEvent row = row(OutboxEventType.ACCOUNT_OWNER_CACHED, owner.accountId(), owner);
        whenPending(List.of(row));

        outboxRelay.drain();

        InOrder inOrder = inOrder(accountCache, outboxEventRepository);
        inOrder.verify(accountCache).saveAccountOwner(owner.accountId(), owner.userId());
        inOrder.verify(outboxEventRepository).delete(row);
        verifyNoInteractions(kafkaTemplate, accountProjector);
    }

    @Test
    void shouldKeepTheOwnerRowPendingWhenTheCacheFails() {
        AccountOwnerDTO owner = new AccountOwnerDTO(UUID.randomUUID(), UUID.randomUUID());
        OutboxEvent row = row(OutboxEventType.ACCOUNT_OWNER_CACHED, owner.accountId(), owner);
        whenPending(List.of(row));
        doThrow(new IllegalStateException("redis fora do ar")).when(accountCache).saveAccountOwner(any(), any());

        outboxRelay.drain();

        verify(outboxEventRepository, never()).delete(any(OutboxEvent.class));
    }

    @Test
    void shouldKeepTheRowAndStopWhenTheDestinationFails() {
        AccountDocument first = new AccountDocument();
        first.setId(UUID.randomUUID());
        AccountDocument second = new AccountDocument();
        second.setId(UUID.randomUUID());
        OutboxEvent firstRow = row(OutboxEventType.ACCOUNT_CHANGED, first.getId(), first);
        OutboxEvent secondRow = row(OutboxEventType.ACCOUNT_CHANGED, second.getId(), second);
        whenPending(List.of(firstRow, secondRow));
        doThrow(new IllegalStateException("mongo fora do ar")).when(accountProjector).project(any());

        outboxRelay.drain();

        verify(accountProjector, times(1)).project(any());
        verify(outboxEventRepository, never()).delete(any(OutboxEvent.class));
    }

    @Test
    void shouldDoNothingWhenThereIsNothingPending() {
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of());

        outboxRelay.drain();

        verifyNoInteractions(kafkaTemplate, accountProjector);
    }

    @Test
    void shouldKeepGoingUntilTheOutboxIsEmpty() {
        AccountDocument snapshot = new AccountDocument();
        snapshot.setId(UUID.randomUUID());
        OutboxEvent row = row(OutboxEventType.ACCOUNT_CHANGED, snapshot.getId(), snapshot);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc())
                .thenReturn(List.of(row))
                .thenReturn(List.of());

        outboxRelay.drain();

        verify(outboxEventRepository, times(2)).findTop100ByOrderByCreatedAtAsc();
        verify(accountProjector, times(1)).project(any());
    }

    private void whenPending(List<OutboxEvent> rows) {
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc())
                .thenReturn(rows)
                .thenReturn(List.of());
    }

    private OutboxEvent row(OutboxEventType type, UUID accountId, Object payload) {
        OutboxEvent row = new OutboxEvent();
        row.setId(UUID.randomUUID());
        row.setType(type);
        row.setAggregateId(accountId.toString());
        row.setPayload(objectMapper.valueToTree(payload));
        return row;
    }
}
