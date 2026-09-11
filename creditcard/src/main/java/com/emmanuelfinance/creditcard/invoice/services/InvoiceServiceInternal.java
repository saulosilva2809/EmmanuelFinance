package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceServiceInternal {

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
