package com.emmanuelfinance.shared.kafka;

import com.emmanuelfinance.shared.enums.FailedOperationEnum;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.*;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailedEventTest {

    private final UUID id = UUID.randomUUID();

    private final TransactionDeletedAndRestoreEvent deletedOrRestored = new TransactionDeletedAndRestoreEvent(
            id, UUID.randomUUID(), null, UUID.randomUUID(), BigDecimal.TEN, 1,
            TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID());

    @Test
    void shouldTellDeletionFromRestoreByTheTopic() {
        assertEquals(FailedOperationEnum.DELETE,
                TransactionFailedEvent.from("transaction-deleted-topic", deletedOrRestored).operation());
        assertEquals(FailedOperationEnum.RESTORE,
                TransactionFailedEvent.from("transaction-restore-topic", deletedOrRestored).operation());
    }

    @Test
    void shouldCarryTheOldValuesOfAFailedUpdate() {
        UUID oldAccount = UUID.randomUUID();
        TransactionUpdatedEvent updated = new TransactionUpdatedEvent(
                id, oldAccount, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN,
                TypeTransactionEnum.EXPENSE, TypeTransactionEnum.INCOME,
                StatusTransactionEnum.PAID, StatusTransactionEnum.PAID, UUID.randomUUID());

        TransactionFailedEvent failed = TransactionFailedEvent.from("transaction-updated-topic", updated);

        assertEquals(new TransactionFailedEvent(id, FailedOperationEnum.UPDATE, oldAccount, BigDecimal.ONE,
                TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID), failed);
    }

    @Test
    void shouldDescribeAFailedTransferCreation() {
        TransferCreatedEventDTO created = new TransferCreatedEventDTO(
                id, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, UUID.randomUUID());

        assertEquals(FailedOperationEnum.CREATE,
                TransferFailedEvent.from("transfer-created-topic", created).operation());
    }

    // é a mesma configuração dos yaml: só classes do sistema podem sair do kafka
    @Test
    void shouldReadOurEventsWithTheTrustedPackagesOfTheServices() {
        TransactionFailedEvent event = TransactionFailedEvent.from("transaction-created-topic", deletedOrRestored);
        RecordHeaders headers = new RecordHeaders();

        try (JsonSerializer<Object> serializer = new JsonSerializer<>();
             JsonDeserializer<Object> deserializer = new JsonDeserializer<>()) {
            deserializer.configure(Map.of(
                    JsonDeserializer.TRUSTED_PACKAGES, "com.emmanuelfinance.*",
                    JsonDeserializer.USE_TYPE_INFO_HEADERS, true), false);

            byte[] bytes = serializer.serialize("transaction-failed-topic", headers, event);

            assertEquals(event, deserializer.deserialize("transaction-failed-topic", headers, bytes));
        }
    }
}
