package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemProjector;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceItemOutboxEventType;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceOutboxEventType;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvoiceItemOutboxDispatcherTest {

    @Mock
    private InvoiceItemProjector invoiceItemProjector;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private InvoiceItemOutboxDispatcher dispatcher;

    @Test
    void shouldSupportOnlyTheInvoiceItemChangedType() {
        assertTrue(dispatcher.supports(InvoiceItemOutboxEventType.INVOICE_ITEM_CHANGED));
        assertFalse(dispatcher.supports(InvoiceOutboxEventType.INVOICE_CHANGED));
    }

    @Test
    void shouldRecognizeTheTypeReadBackFromTheDatabase() {
        assertTrue(dispatcher.supports(row(newSnapshot()).getType()));
    }

    @Test
    void shouldApplyTheSnapshotInMongo() throws Exception {
        InvoiceItemDocument snapshot = newSnapshot();

        dispatcher.dispatch(row(snapshot));

        verify(invoiceItemProjector).project(argThat(received ->
                received.getId().equals(snapshot.getId())
                        && received.getInvoiceId().equals(snapshot.getInvoiceId())
                        && received.getInstallmentNumber() == 2
                        && received.getAmount().compareTo(new BigDecimal("100.00")) == 0
                        && received.isDeleted()));
    }

    @Test
    void shouldLetTheFailureOfMongoGoUpSoTheRowStaysPending() {
        doThrow(new IllegalStateException("mongo fora do ar")).when(invoiceItemProjector).project(any());

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(row(newSnapshot())));
    }

    private InvoiceItemDocument newSnapshot() {
        InvoiceItemDocument snapshot = new InvoiceItemDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setUserId(UUID.randomUUID());
        snapshot.setInvoiceId(UUID.randomUUID());
        snapshot.setTransactionId(UUID.randomUUID());
        snapshot.setInstallmentNumber(2);
        snapshot.setTotalInstallments(3);
        snapshot.setAmount(new BigDecimal("100.00"));
        snapshot.setDeleted(true);
        return snapshot;
    }

    private OutboxEvent row(InvoiceItemDocument snapshot) {
        OutboxEvent row = new OutboxEvent();
        row.setType(InvoiceItemOutboxEventType.INVOICE_ITEM_CHANGED);
        row.setAggregateId(snapshot.getId().toString());
        row.setPayload(objectMapper.valueToTree(snapshot));
        return row;
    }
}
