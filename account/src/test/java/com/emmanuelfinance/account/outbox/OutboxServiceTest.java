package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        OutboxEventFactory outboxEventFactory = new OutboxEventFactory(
                JsonMapper.builder().findAndAddModules().build(),
                Mappers.getMapper(AccountMapper.class)
        );
        outboxService = new OutboxService(outboxEventRepository, applicationEventPublisher, outboxEventFactory);
    }

    @Test
    void shouldRegisterTheEventTheSnapshotAndTheOwnerCacheWhenTheAccountIsCreated() {
        outboxService.accountCreated(newAccount());

        assertEquals(List.of(
                AccountOutboxEventType.ACCOUNT_CREATED,
                AccountOutboxEventType.ACCOUNT_CHANGED,
                AccountOutboxEventType.ACCOUNT_OWNER_CACHED
        ), registeredTypes());
    }

    @Test
    void shouldRegisterOnlyTheSnapshotWhenTheAccountIsUpdated() {
        outboxService.accountUpdated(newAccount());

        assertEquals(List.of(AccountOutboxEventType.ACCOUNT_CHANGED), registeredTypes());
    }

    @Test
    void shouldRegisterTheEventAndTheSnapshotWhenTheAccountIsDeleted() {
        outboxService.accountDeleted(newAccount());

        assertEquals(List.of(AccountOutboxEventType.ACCOUNT_DELETED, AccountOutboxEventType.ACCOUNT_CHANGED), registeredTypes());
    }

    @Test
    void shouldRegisterTheEventAndTheSnapshotWhenTheAccountIsRestored() {
        outboxService.accountRestored(newAccount());

        assertEquals(List.of(AccountOutboxEventType.ACCOUNT_RESTORED, AccountOutboxEventType.ACCOUNT_CHANGED), registeredTypes());
    }

    @Test
    void shouldRegisterOnlyTheSnapshotWhenTheBalanceChanges() {
        outboxService.accountBalanceChanged(newAccount());

        assertEquals(List.of(AccountOutboxEventType.ACCOUNT_CHANGED), registeredTypes());
    }

    @Test
    void shouldWakeTheRelayOnlyOncePerOperation() {
        outboxService.accountCreated(newAccount());

        verify(applicationEventPublisher, times(1)).publishEvent(any(OutboxEventSavedDTO.class));
    }

    // todas as linhas de uma operação são gravadas juntas, em uma única chamada
    @SuppressWarnings("unchecked")
    private List<AccountOutboxEventType> registeredTypes() {
        ArgumentCaptor<List<OutboxEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxEventRepository, times(1)).saveAll(captor.capture());

        return captor.getValue().stream()
                .map(event -> (AccountOutboxEventType) event.getType())
                .toList();
    }

    private Account newAccount() {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUserId(UUID.randomUUID());
        account.setName("Caixa");
        account.setType(TypeEnum.CASH);
        account.setCurrentBalance(new BigDecimal("50.00"));
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}
