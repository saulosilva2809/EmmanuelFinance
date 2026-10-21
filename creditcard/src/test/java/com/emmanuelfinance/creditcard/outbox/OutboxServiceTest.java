package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardMapper;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocument;
import com.emmanuelfinance.creditcard.outbox.enums.CreditCardOutboxEventType;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        OutboxEventFactory outboxEventFactory =
                new OutboxEventFactory(objectMapper, Mappers.getMapper(CreditCardMapper.class));
        outboxService = new OutboxService(outboxEventRepository, applicationEventPublisher, outboxEventFactory);
    }

    @Test
    void shouldRegisterOnlyTheInvoicePaymentRowInASingleCall() {
        outboxService.invoicePayment(newPayment());

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(CreditCardOutboxEventType.INVOICE_PAYMENT, saved.get(0).getType());
    }

    @Test
    void shouldRegisterTheSnapshotWhenTheCreditCardChanges() throws Exception {
        outboxService.creditCardChanged(newCreditCard());

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(CreditCardOutboxEventType.CREDIT_CARD_CHANGED, saved.get(0).getType());
        assertFalse(objectMapper.treeToValue(saved.get(0).getPayload(), CreditCardDocument.class).isDeleted());
    }

    @Test
    void shouldRegisterTheSnapshotMarkedAsDeletedWhenTheCreditCardIsDeleted() throws Exception {
        outboxService.creditCardDeleted(newCreditCard());

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(CreditCardOutboxEventType.CREDIT_CARD_CHANGED, saved.get(0).getType());
        assertTrue(objectMapper.treeToValue(saved.get(0).getPayload(), CreditCardDocument.class).isDeleted());
    }

    @Test
    void shouldWakeTheRelayOncePerOperation() {
        outboxService.creditCardChanged(newCreditCard());

        verify(applicationEventPublisher, times(1)).publishEvent(any(OutboxEventSavedDTO.class));
    }

    @SuppressWarnings("unchecked")
    private List<OutboxEvent> registeredRows() {
        ArgumentCaptor<List<OutboxEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxEventRepository, times(1)).saveAll(captor.capture());
        return captor.getValue();
    }

    private CreditCard newCreditCard() {
        return CreditCardTestDataBuilder.createEntity(
                CreditCardTestDataBuilder.createCardDTO(), UUID.randomUUID(), false);
    }

    private InvoicePaymentDTO newPayment() {
        return new InvoicePaymentDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Nubank",
                UUID.randomUUID(), 10, 2026, new BigDecimal("300.00"));
    }
}
