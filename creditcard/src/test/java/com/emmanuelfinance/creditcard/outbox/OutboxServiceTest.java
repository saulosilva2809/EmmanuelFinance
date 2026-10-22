package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardMapper;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocument;
import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemDocument;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceItemOutboxEventType;
import com.emmanuelfinance.creditcard.outbox.enums.InvoiceOutboxEventType;
import com.emmanuelfinance.creditcard.invoice.InvoiceMapper;
import com.emmanuelfinance.creditcard.outbox.enums.CreditCardOutboxEventType;
import com.emmanuelfinance.creditcard.outbox.enums.PaymentOutboxEventType;
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
                new OutboxEventFactory(objectMapper, Mappers.getMapper(CreditCardMapper.class), Mappers.getMapper(InvoiceMapper.class));
        outboxService = new OutboxService(outboxEventRepository, applicationEventPublisher, outboxEventFactory);
    }

    @Test
    void shouldRegisterOnlyTheInvoicePaymentRowInASingleCall() {
        outboxService.invoicePayment(newPayment());

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(PaymentOutboxEventType.INVOICE_PAYMENT.name(), saved.get(0).getType().name());
    }

    @Test
    void shouldRegisterTheSnapshotWhenTheCreditCardChanges() throws Exception {
        outboxService.creditCardChanged(newCreditCard());

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(CreditCardOutboxEventType.CREDIT_CARD_CHANGED.name(), saved.get(0).getType().name());
        assertFalse(objectMapper.treeToValue(saved.get(0).getPayload(), CreditCardDocument.class).isDeleted());
    }

    @Test
    void shouldRegisterTheSnapshotMarkedAsDeletedWhenTheCreditCardIsDeleted() throws Exception {
        outboxService.creditCardDeleted(newCreditCard());

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(CreditCardOutboxEventType.CREDIT_CARD_CHANGED.name(), saved.get(0).getType().name());
        assertTrue(objectMapper.treeToValue(saved.get(0).getPayload(), CreditCardDocument.class).isDeleted());
    }

    @Test
    void shouldWakeTheRelayOncePerOperation() {
        outboxService.creditCardChanged(newCreditCard());

        verify(applicationEventPublisher, times(1)).publishEvent(any(OutboxEventSavedDTO.class));
    }

    @Test
    void shouldRegisterTheInvoiceSnapshotWhenTheInvoiceChangesOrIsDeleted() throws Exception {
        Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), UUID.randomUUID());

        outboxService.invoiceChanged(invoice);
        outboxService.invoiceDeleted(invoice);

        ArgumentCaptor<List<OutboxEvent>> captor = rowsCaptor();
        verify(outboxEventRepository, times(2)).saveAll(captor.capture());
        List<OutboxEvent> first = captor.getAllValues().get(0);
        List<OutboxEvent> second = captor.getAllValues().get(1);
        assertEquals(InvoiceOutboxEventType.INVOICE_CHANGED.name(), first.get(0).getType().name());
        assertFalse(objectMapper.treeToValue(first.get(0).getPayload(), InvoiceDocument.class).isDeleted());
        assertTrue(objectMapper.treeToValue(second.get(0).getPayload(), InvoiceDocument.class).isDeleted());
    }

    @Test
    void shouldRegisterTheInvoiceItemSnapshotWhenTheItemChangesOrIsDeleted() throws Exception {
        InvoiceItem item = InvoiceTestDataBuilder.invoiceItemEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        outboxService.invoiceItemChanged(item);
        outboxService.invoiceItemDeleted(item);

        ArgumentCaptor<List<OutboxEvent>> captor = rowsCaptor();
        verify(outboxEventRepository, times(2)).saveAll(captor.capture());
        List<OutboxEvent> first = captor.getAllValues().get(0);
        List<OutboxEvent> second = captor.getAllValues().get(1);
        assertEquals(InvoiceItemOutboxEventType.INVOICE_ITEM_CHANGED.name(), first.get(0).getType().name());
        assertFalse(objectMapper.treeToValue(first.get(0).getPayload(), InvoiceItemDocument.class).isDeleted());
        assertTrue(objectMapper.treeToValue(second.get(0).getPayload(), InvoiceItemDocument.class).isDeleted());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<OutboxEvent>> rowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
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
