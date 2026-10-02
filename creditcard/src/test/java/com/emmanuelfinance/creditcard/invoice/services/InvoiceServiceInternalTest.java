package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceInternalTest {

    @Mock
    private InvoiceSelector invoiceSelector;

    @InjectMocks
    private InvoiceServiceInternal invoiceServiceInternal;

    @Test
    @DisplayName("Deve mapear a fatura para o DTO de resumo")
    void shouldMapInvoiceToSummaryDTO() {
        Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), UUID.randomUUID());
        when(invoiceSelector.getById(invoice.getId())).thenReturn(invoice);

        ResponseInvoiceSummaryDTO result = invoiceServiceInternal.invoiceSummaryDTO(invoice.getId());

        assertEquals(InvoiceTestDataBuilder.responseInvoiceSummaryDTO(invoice), result);
        verify(invoiceSelector, times(1)).getById(invoice.getId());
    }

    @Test
    @DisplayName("Deve propagar a exceção do selector")
    void shouldPropagateSelectorException() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceSelector.getById(invoiceId)).thenThrow(new IllegalStateException("not found"));

        assertThrows(IllegalStateException.class, () -> invoiceServiceInternal.invoiceSummaryDTO(invoiceId));
    }
}
