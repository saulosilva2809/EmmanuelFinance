package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.outbox.dto.AccountOwnerDTO;
import com.emmanuelfinance.account.outbox.enums.OutboxEventType;
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
    void shouldBuildTheShortEventForTheOtherServices() throws Exception {
        Account account = newAccount();

        OutboxEvent row = outboxEventFactory.accountEvent(account, StatusEventEnum.CREATED);

        assertEquals(OutboxEventType.ACCOUNT_EVENT, row.getType());
        assertEquals(account.getId().toString(), row.getAggregateId());
        assertNotNull(row.getCreatedAt());
        assertEquals(new AccountEventDTO(account.getId(), account.getUserId(), StatusEventEnum.CREATED),
                objectMapper.treeToValue(row.getPayload(), AccountEventDTO.class));
    }

    @Test
    void shouldBuildTheSnapshotWithTheWholeAccount() throws Exception {
        Account account = newAccount();

        OutboxEvent row = outboxEventFactory.accountSnapshot(account, false);

        assertEquals(OutboxEventType.ACCOUNT_CHANGED, row.getType());
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

    @Test
    void shouldBuildTheOwnerCacheRow() throws Exception {
        Account account = newAccount();

        OutboxEvent row = outboxEventFactory.accountOwner(account);

        assertEquals(OutboxEventType.ACCOUNT_OWNER_CACHED, row.getType());
        assertEquals(new AccountOwnerDTO(account.getId(), account.getUserId()),
                objectMapper.treeToValue(row.getPayload(), AccountOwnerDTO.class));
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
