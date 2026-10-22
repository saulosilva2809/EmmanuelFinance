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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventFactoryTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final OutboxEventFactory outboxEventFactory =
            new OutboxEventFactory(objectMapper, Mappers.getMapper(CreditCardMapper.class), Mappers.getMapper(InvoiceMapper.class));

    @Test
    void shouldBuildTheInvoicePaymentRowWithThePaymentAsPayload() throws Exception {
        InvoicePaymentDTO payment = newPayment();

        OutboxEvent row = outboxEventFactory.createdInvoicePayment(payment);

        assertEquals(PaymentOutboxEventType.INVOICE_PAYMENT.name(), row.getType().name());
        assertEquals(payment.accountId().toString(), row.getAggregateId());
        assertNotNull(row.getCreatedAt());

        InvoicePaymentDTO saved = objectMapper.treeToValue(row.getPayload(), InvoicePaymentDTO.class);
        assertEquals(payment.accountId(), saved.accountId());
        assertEquals(payment.userId(), saved.userId());
        assertEquals(payment.creditCardId(), saved.creditCardId());
        assertEquals(payment.creditCardName(), saved.creditCardName());
        assertEquals(payment.invoiceId(), saved.invoiceId());
        assertEquals(payment.month(), saved.month());
        assertEquals(payment.year(), saved.year());
        // o Jackson tira os zeros finais do BigDecimal (300.00 vira 3E+2): o valor é o mesmo, só a escala muda
        assertEquals(0, payment.totalPaid().compareTo(saved.totalPaid()));
    }

    @Test
    void shouldBuildTheCreditCardSnapshotWithTheWholeCard() throws Exception {
        CreditCard creditCard = newCreditCard();

        OutboxEvent row = outboxEventFactory.creditCardSnapshot(creditCard, false);

        assertEquals(CreditCardOutboxEventType.CREDIT_CARD_CHANGED.name(), row.getType().name());
        assertEquals(creditCard.getId().toString(), row.getAggregateId());
        assertNotNull(row.getCreatedAt());

        CreditCardDocument snapshot = objectMapper.treeToValue(row.getPayload(), CreditCardDocument.class);
        assertEquals(creditCard.getId(), snapshot.getId());
        assertEquals(creditCard.getUserId(), snapshot.getUserId());
        assertEquals(creditCard.getAccountId(), snapshot.getAccountId());
        assertEquals(creditCard.getName(), snapshot.getName());
        assertEquals(creditCard.getCreditLimit(), snapshot.getCreditLimit());
        assertEquals(new BigDecimal("4500.00"), snapshot.getAvailableLimit());
        assertEquals(creditCard.getClosingDay(), snapshot.getClosingDay());
        assertEquals(creditCard.getDueDay(), snapshot.getDueDay());
        assertFalse(snapshot.isDeleted());
    }

    @Test
    void shouldMarkTheSnapshotAsDeletedWhenAskedTo() throws Exception {
        OutboxEvent row = outboxEventFactory.creditCardSnapshot(newCreditCard(), true);

        assertTrue(objectMapper.treeToValue(row.getPayload(), CreditCardDocument.class).isDeleted());
    }

    @Test
    void shouldBuildTheInvoiceSnapshotWithTheWholeInvoice() throws Exception {
        Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), UUID.randomUUID());

        OutboxEvent row = outboxEventFactory.invoiceSnapshot(invoice, false);

        assertEquals(InvoiceOutboxEventType.INVOICE_CHANGED.name(), row.getType().name());
        assertEquals(invoice.getId().toString(), row.getAggregateId());

        InvoiceDocument snapshot = objectMapper.treeToValue(row.getPayload(), InvoiceDocument.class);
        assertEquals(invoice.getId(), snapshot.getId());
        assertEquals(invoice.getUserId(), snapshot.getUserId());
        assertEquals(invoice.getCreditCardId(), snapshot.getCreditCardId());
        assertEquals(invoice.getMonth(), snapshot.getMonth());
        assertEquals(invoice.getYear(), snapshot.getYear());
        assertEquals(invoice.getStatus(), snapshot.getStatus());
        assertEquals(0, invoice.getTotalAmount().compareTo(snapshot.getTotalAmount()));
        assertFalse(snapshot.isDeleted());
    }

    @Test
    void shouldMarkTheInvoiceSnapshotAsDeletedWhenAskedTo() throws Exception {
        Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), UUID.randomUUID());

        OutboxEvent row = outboxEventFactory.invoiceSnapshot(invoice, true);

        assertTrue(objectMapper.treeToValue(row.getPayload(), InvoiceDocument.class).isDeleted());
    }

    @Test
    void shouldBuildTheInvoiceItemSnapshotWithTheWholeItem() throws Exception {
        InvoiceItem item = InvoiceTestDataBuilder.invoiceItemEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        OutboxEvent row = outboxEventFactory.invoiceItemSnapshot(item, false);

        assertEquals(InvoiceItemOutboxEventType.INVOICE_ITEM_CHANGED.name(), row.getType().name());
        assertEquals(item.getId().toString(), row.getAggregateId());

        InvoiceItemDocument snapshot = objectMapper.treeToValue(row.getPayload(), InvoiceItemDocument.class);
        assertEquals(item.getId(), snapshot.getId());
        assertEquals(item.getUserId(), snapshot.getUserId());
        assertEquals(item.getInvoiceId(), snapshot.getInvoiceId());
        assertEquals(item.getTransactionId(), snapshot.getTransactionId());
        assertEquals(item.getInstallmentNumber(), snapshot.getInstallmentNumber());
        assertEquals(item.getTotalInstallments(), snapshot.getTotalInstallments());
        assertEquals(0, item.getAmount().compareTo(snapshot.getAmount()));
        assertFalse(snapshot.isDeleted());
    }

    @Test
    void shouldMarkTheInvoiceItemSnapshotAsDeletedWhenAskedTo() throws Exception {
        InvoiceItem item = InvoiceTestDataBuilder.invoiceItemEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        OutboxEvent row = outboxEventFactory.invoiceItemSnapshot(item, true);

        assertTrue(objectMapper.treeToValue(row.getPayload(), InvoiceItemDocument.class).isDeleted());
    }

    private CreditCard newCreditCard() {
        CreditCard creditCard = CreditCardTestDataBuilder.createEntity(
                CreditCardTestDataBuilder.createCardDTO(), UUID.randomUUID(), false);
        creditCard.setAvailableLimit(new BigDecimal("4500.00"));
        return creditCard;
    }

    private InvoicePaymentDTO newPayment() {
        return new InvoicePaymentDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Nubank",
                UUID.randomUUID(), 10, 2026, new BigDecimal("300.00"));
    }
}
