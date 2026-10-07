package com.emmanuelfinance.creditcard.invoice.selectors;

import  com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceSelector {

    private final InvoiceRepository invoiceRepository;

    public Optional<Invoice> getByCreditCardAndMonthAndYear(
            UUID creditCardId,
            Integer month,
            Integer year
    ) {
        Optional<Invoice> invoice = invoiceRepository.findByCreditCardIdAndMonthAndYearAndDeletedFalse(
                creditCardId,
                month,
                year
        );

        return invoice;
    }

    public Invoice getById(UUID invoiceId) {
        return invoiceRepository.getById(invoiceId);
    }

    public Invoice getByIdExcludingDeleted(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(invoiceId)
                .orElseThrow(() -> new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND));

        return invoice;
    }
}
