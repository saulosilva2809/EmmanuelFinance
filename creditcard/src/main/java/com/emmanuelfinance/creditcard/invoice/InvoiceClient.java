package com.emmanuelfinance.creditcard.invoice;

import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceClient {

    private final InvoiceSelector invoiceSelector;

    public ResponseInvoiceSummaryDTO invoiceSummaryDTO(UUID invoiceId) {
        Invoice invoice = invoiceSelector.getById(invoiceId);

        return new ResponseInvoiceSummaryDTO(
                invoice.getId(),
                invoice.getMonth(),
                invoice.getYear(),
                invoice.getTotalAmount(),
                invoice.getStatus()
        );
    }
}
