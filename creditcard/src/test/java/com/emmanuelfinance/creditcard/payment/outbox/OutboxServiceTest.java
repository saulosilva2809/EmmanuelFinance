package com.emmanuelfinance.creditcard.payment.outbox;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
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
        OutboxEventFactory outboxEventFactory = new OutboxEventFactory(JsonMapper.builder().findAndAddModules().build());
        outboxService = new OutboxService(outboxEventRepository, applicationEventPublisher, outboxEventFactory);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRegisterOnlyTheInvoicePaymentRowInASingleCall() {
        outboxService.invoicePayment(newPayment());

        ArgumentCaptor<List<OutboxEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxEventRepository, times(1)).saveAll(captor.capture());

        List<OutboxEvent> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(PaymentOutboxEventType.INVOICE_PAYMENT, saved.get(0).getType());
    }

    @Test
    void shouldWakeTheRelayOnce() {
        outboxService.invoicePayment(newPayment());

        verify(applicationEventPublisher, times(1)).publishEvent(any(OutboxEventSavedDTO.class));
    }

    private InvoicePaymentDTO newPayment() {
        return new InvoicePaymentDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Nubank",
                UUID.randomUUID(), 10, 2026, new BigDecimal("300.00"));
    }
}
