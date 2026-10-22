package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceProjector;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceOutboxEventType;
import com.emmanuelfinance.creditcard.outbox.enums.PaymentOutboxEventType;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
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
class InvoiceOutboxDispatcherTest {

    @Mock
    private InvoiceProjector invoiceProjector;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private InvoiceOutboxDispatcher dispatcher;

    @Test
    void shouldSupportOnlyTheInvoiceChangedType() {
        assertTrue(dispatcher.supports(InvoiceOutboxEventType.INVOICE_CHANGED));
        assertFalse(dispatcher.supports(PaymentOutboxEventType.INVOICE_PAYMENT));
    }

    @Test
    void shouldRecognizeTheTypeReadBackFromTheDatabase() {
        assertTrue(dispatcher.supports(row(newSnapshot()).getType()));
    }

    @Test
    void shouldApplyTheSnapshotInMongo() throws Exception {
        InvoiceDocument snapshot = newSnapshot();

        dispatcher.dispatch(row(snapshot));

        verify(invoiceProjector).project(argThat(received ->
                received.getId().equals(snapshot.getId())
                        && received.getStatus() == InvoiceStatusEnum.OPEN
                        && received.getTotalAmount().compareTo(new BigDecimal("300.00")) == 0
                        && received.isDeleted()));
    }

    @Test
    void shouldLetTheFailureOfMongoGoUpSoTheRowStaysPending() {
        doThrow(new IllegalStateException("mongo fora do ar")).when(invoiceProjector).project(any());

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(row(newSnapshot())));
    }

    private InvoiceDocument newSnapshot() {
        InvoiceDocument snapshot = new InvoiceDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setUserId(UUID.randomUUID());
        snapshot.setCreditCardId(UUID.randomUUID());
        snapshot.setStatus(InvoiceStatusEnum.OPEN);
        snapshot.setTotalAmount(new BigDecimal("300.00"));
        snapshot.setDeleted(true);
        return snapshot;
    }

    private OutboxEvent row(InvoiceDocument snapshot) {
        OutboxEvent row = new OutboxEvent();
        row.setType(InvoiceOutboxEventType.INVOICE_CHANGED);
        row.setAggregateId(snapshot.getId().toString());
        row.setPayload(objectMapper.valueToTree(snapshot));
        return row;
    }
}
