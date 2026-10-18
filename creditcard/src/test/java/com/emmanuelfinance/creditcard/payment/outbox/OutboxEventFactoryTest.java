package com.emmanuelfinance.creditcard.payment.outbox;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventFactoryTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final OutboxEventFactory outboxEventFactory = new OutboxEventFactory(objectMapper);

    @Test
    void shouldBuildTheInvoicePaymentRowWithThePaymentAsPayload() throws Exception {
        InvoicePaymentDTO payment = newPayment();

        OutboxEvent row = outboxEventFactory.createdInvoicePayment(payment);

        assertEquals(PaymentOutboxEventType.INVOICE_PAYMENT, row.getType());
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

    private InvoicePaymentDTO newPayment() {
        return new InvoicePaymentDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Nubank",
                UUID.randomUUID(), 10, 2026, new BigDecimal("300.00"));
    }
}
