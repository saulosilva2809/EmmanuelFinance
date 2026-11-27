package com.emmanuelfinance.transaction.transfer.outbox;

import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferDeletedAndRestoreEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferUpdatedEventDTO;
import com.emmanuelfinance.transaction.outbox.OutboxEvent;
import com.emmanuelfinance.transaction.outbox.OutboxEventFactory;
import com.emmanuelfinance.transaction.outbox.OutboxService;
import com.emmanuelfinance.transaction.transfer.Transfer;
import com.emmanuelfinance.transaction.transfer.TransferMapper;
import com.emmanuelfinance.transaction.transfer.TransferTestData;
import com.emmanuelfinance.transaction.transfer.kafka.TransferProducer;
import com.emmanuelfinance.transaction.transfer.query.TransferDocument;
import com.emmanuelfinance.transaction.transfer.query.TransferProjector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferOutboxTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final OutboxService outboxService = mock(OutboxService.class);
    private final TransferOutbox transferOutbox = new TransferOutbox(
            outboxService, new OutboxEventFactory(objectMapper), Mappers.getMapper(TransferMapper.class));
    private final TransferProducer producer = mock(TransferProducer.class);
    private final TransferProjector projector = mock(TransferProjector.class);
    private final TransferEventOutboxDispatcher kafka = new TransferEventOutboxDispatcher(objectMapper, producer);
    private final TransferOutboxDispatcher mongo = new TransferOutboxDispatcher(objectMapper, projector);

    @Test
    void shouldDeliverTheSnapshotToMongo() throws Exception {
        Transfer transfer = TransferTestData.paidTransfer();
        transferOutbox.transferDeleted(transfer);
        OutboxEvent row = registeredRows(1).get(0);

        assertTrue(mongo.supports(row.getType()));
        assertFalse(kafka.supports(row.getType()));
        mongo.dispatch(row);

        ArgumentCaptor<TransferDocument> snapshot = ArgumentCaptor.forClass(TransferDocument.class);
        verify(projector).project(snapshot.capture());
        assertEquals(transfer.getId(), snapshot.getValue().getId());
        assertEquals(transfer.getAmount(), snapshot.getValue().getAmount());
        assertEquals(transfer.getStatus(), snapshot.getValue().getStatus());
        assertTrue(snapshot.getValue().isDeleted());
    }

    @Test
    void shouldSendEachEventToItsTopic() throws Exception {
        UUID id = UUID.randomUUID();
        TransferCreatedEventDTO created = new TransferCreatedEventDTO(
                id, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.55"), UUID.randomUUID());
        TransferUpdatedEventDTO updated = new TransferUpdatedEventDTO(id, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1.25"), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.55"), UUID.randomUUID());
        TransferDeletedAndRestoreEventDTO event = new TransferDeletedAndRestoreEventDTO(
                id, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.55"), UUID.randomUUID());

        transferOutbox.transferCreatedEvent(created);
        transferOutbox.transferUpdatedEvent(updated);
        transferOutbox.transferDeletedEvent(event);
        transferOutbox.transferRestoredEvent(event);
        for (OutboxEvent row : registeredRows(4)) {
            kafka.dispatch(row);
        }

        verify(producer).publishTransferCreated(created);
        verify(producer).publishTransferUpdated(updated);
        verify(producer).publishTransferDeleted(event);
        verify(producer).publishTransferRestore(event);
    }

    private List<OutboxEvent> registeredRows(int expected) {
        ArgumentCaptor<OutboxEvent> rows = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService, times(expected)).register(rows.capture());
        return rows.getAllValues();
    }
}
