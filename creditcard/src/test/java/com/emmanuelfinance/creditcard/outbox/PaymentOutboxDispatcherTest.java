package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.outbox.enums.PaymentOutboxEventType;
import com.emmanuelfinance.creditcard.payment.kafka.PaymentProducer;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxDispatcherTest {

    @Mock
    private PaymentProducer paymentProducer;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private PaymentOutboxDispatcher dispatcher;

    @Test
    void shouldSupportOnlyTheInvoicePaymentType() {
        assertTrue(dispatcher.supports(PaymentOutboxEventType.INVOICE_PAYMENT));
        assertFalse(dispatcher.supports(() -> "OUTRO_TIPO"));
    }

    @Test
    void shouldPublishTheInvoicePaymentWithTheSameData() throws Exception {
        InvoicePaymentDTO payment = newPayment();

        dispatcher.dispatch(row(payment));

        ArgumentCaptor<InvoicePaymentDTO> captor = ArgumentCaptor.forClass(InvoicePaymentDTO.class);
        verify(paymentProducer).publishInvoicePaymentCreated(captor.capture());

        InvoicePaymentDTO published = captor.getValue();
        assertEquals(payment.accountId(), published.accountId());
        assertEquals(payment.userId(), published.userId());
        assertEquals(payment.creditCardId(), published.creditCardId());
        assertEquals(payment.creditCardName(), published.creditCardName());
        assertEquals(payment.invoiceId(), published.invoiceId());
        assertEquals(payment.month(), published.month());
        assertEquals(payment.year(), published.year());
        assertEquals(0, payment.totalPaid().compareTo(published.totalPaid()));
    }

    @Test
    void shouldLetTheFailureOfTheProducerGoUpSoTheRowStaysPending() {
        doThrow(new IllegalStateException("kafka fora do ar")).when(paymentProducer).publishInvoicePaymentCreated(any());

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(row(newPayment())));
    }

    private OutboxEvent row(InvoicePaymentDTO payment) {
        OutboxEvent row = new OutboxEvent();
        row.setType(PaymentOutboxEventType.INVOICE_PAYMENT);
        row.setAggregateId(payment.accountId().toString());
        row.setPayload(objectMapper.valueToTree(payment));
        return row;
    }

    private InvoicePaymentDTO newPayment() {
        return new InvoicePaymentDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Nubank",
                UUID.randomUUID(), 10, 2026, new BigDecimal("300.00"), UUID.randomUUID());
    }
}
