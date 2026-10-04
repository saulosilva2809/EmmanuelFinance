package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceValidatorService {

    private final InvoiceRepository invoiceRepository;

    public void existsById(UUID invoiceId) {
        boolean exists = invoiceRepository.existsByIdAndDeletedFalse(invoiceId);

        if (Boolean.FALSE.equals(exists)) {
            throw new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND);
        }
    }
}
