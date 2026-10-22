package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocument;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardProjector;
import com.emmanuelfinance.creditcard.outbox.enums.CreditCardOutboxEventType;
import com.emmanuelfinance.creditcard.outbox.enums.PaymentOutboxEventType;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
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
class CreditCardOutboxDispatcherTest {

    @Mock
    private CreditCardProjector creditCardProjector;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private CreditCardOutboxDispatcher dispatcher;

    @Test
    void shouldSupportOnlyTheCreditCardChangedType() {
        assertTrue(dispatcher.supports(CreditCardOutboxEventType.CREDIT_CARD_CHANGED));
        assertFalse(dispatcher.supports(PaymentOutboxEventType.INVOICE_PAYMENT));
        assertFalse(dispatcher.supports(() -> "OUTRO_TIPO"));
    }

    @Test
    void shouldApplyTheSnapshotInMongo() throws Exception {
        CreditCardDocument snapshot = newSnapshot();

        dispatcher.dispatch(row(snapshot));

        verify(creditCardProjector).project(argThat(received ->
                received.getId().equals(snapshot.getId())
                        && received.getName().equals("Nubank")
                        && received.getAvailableLimit().equals(new BigDecimal("4500.00"))
                        && received.isDeleted()));
    }

    @Test
    void shouldLetTheFailureOfMongoGoUpSoTheRowStaysPending() {
        doThrow(new IllegalStateException("mongo fora do ar")).when(creditCardProjector).project(any());

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(row(newSnapshot())));
    }

    private CreditCardDocument newSnapshot() {
        CreditCardDocument snapshot = new CreditCardDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setUserId(UUID.randomUUID());
        snapshot.setAccountId(UUID.randomUUID());
        snapshot.setName("Nubank");
        snapshot.setAvailableLimit(new BigDecimal("4500.00"));
        snapshot.setCreditLimit(new BigDecimal("5000.00"));
        snapshot.setDeleted(true);
        return snapshot;
    }

    private OutboxEvent row(CreditCardDocument snapshot) {
        OutboxEvent row = new OutboxEvent();
        row.setType(CreditCardOutboxEventType.CREDIT_CARD_CHANGED);
        row.setAggregateId(snapshot.getId().toString());
        row.setPayload(objectMapper.valueToTree(snapshot));
        return row;
    }
}
