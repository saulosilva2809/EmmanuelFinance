package com.emmanuelfinance.creditcard.invoice.selectors;

import com.emmanuelfinance.creditcard.invoice.Invoice;
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
        Optional<Invoice> invoice = invoiceRepository.findByCreditCardIdAndMonthAndYear(
                creditCardId,
                month,
                year
        );

        return invoice;
    }
}
