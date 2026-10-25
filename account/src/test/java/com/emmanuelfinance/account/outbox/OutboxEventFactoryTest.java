package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.outbox.enums.AccountOutboxEventType;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventFactoryTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final OutboxEventFactory outboxEventFactory =
            new OutboxEventFactory(objectMapper, Mappers.getMapper(AccountMapper.class));

    @Test
    void shouldBuildTheCreatedEventForTheOtherServices() throws Exception {
        Account account = newAccount();

        OutboxEvent row = outboxEventFactory.accountCreatedEvent(account);

        assertEquals(AccountOutboxEventType.ACCOUNT_CREATED, row.getType());
        assertEquals(account.getId().toString(), row.getAggregateId());
        assertNotNull(row.getCreatedAt());
        assertEquals(new AccountEventDTO(account.getId(), account.getUserId(), StatusEventEnum.CREATED),
                objectMapper.treeToValue(row.getPayload(), AccountEventDTO.class));
    }

    @Test
    void shouldBuildTheDeletedEventForTheOtherServices() throws Exception {
        Account account = newAccount();

        OutboxEvent row = outboxEventFactory.accountDeletedEvent(account);

        assertEquals(AccountOutboxEventType.ACCOUNT_DELETED, row.getType());
        assertEquals(account.getId().toString(), row.getAggregateId());
        assertEquals(new AccountEventDTO(account.getId(), account.getUserId(), StatusEventEnum.DELETED),
                objectMapper.treeToValue(row.getPayload(), AccountEventDTO.class));
    }

    @Test
    void shouldBuildTheRestoredEventForTheOtherServices() throws Exception {
        Account account = newAccount();

        OutboxEvent row = outboxEventFactory.accountRestoredEvent(account);

        assertEquals(AccountOutboxEventType.ACCOUNT_RESTORED, row.getType());
        assertEquals(account.getId().toString(), row.getAggregateId());
        assertEquals(new AccountEventDTO(account.getId(), account.getUserId(), StatusEventEnum.RESTORE),
                objectMapper.treeToValue(row.getPayload(), AccountEventDTO.class));
    }

    @Test
    void shouldBuildTheSnapshotWithTheWholeAccount() throws Exception {
        Account account = newAccount();

        OutboxEvent row = outboxEventFactory.accountSnapshot(account, false);

        assertEquals(AccountOutboxEventType.ACCOUNT_CHANGED, row.getType());
        assertEquals(account.getId().toString(), row.getAggregateId());

        AccountDocument snapshot = objectMapper.treeToValue(row.getPayload(), AccountDocument.class);
        assertEquals(account.getId(), snapshot.getId());
        assertEquals(account.getUserId(), snapshot.getUserId());
        assertEquals("Caixa", snapshot.getName());
        assertEquals(new BigDecimal("50.00"), snapshot.getCurrentBalance());
        assertFalse(snapshot.isDeleted());
    }

    @Test
    void shouldMarkTheSnapshotAsDeletedWhenAskedTo() throws Exception {
        OutboxEvent row = outboxEventFactory.accountSnapshot(newAccount(), true);

        assertTrue(objectMapper.treeToValue(row.getPayload(), AccountDocument.class).isDeleted());
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
