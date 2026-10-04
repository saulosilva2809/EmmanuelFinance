package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.shared.enums.TypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceAmountService {

    private final InvoiceRepository invoiceRepository;

    private void verifyAmount(List<Invoice> invoiceList) {
        invoiceList.stream()
                .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) == 0)
                .forEach(invoice -> invoice.setDeleted(true));
    }

    public void removeBalanceAfterDeletion(List<Invoice> invoiceList, BigDecimal amount) {
        invoiceList.stream()
                .forEach(invoice -> invoice.setTotalAmount(
                            invoice.getTotalAmount().subtract(amount)
                ));
        verifyAmount(invoiceList);
        invoiceRepository.saveAll(invoiceList);
    }

    public void addBalanceAfterRestore(List<Invoice> invoiceList, BigDecimal amount) {
        invoiceList.forEach(invoice -> {
                invoice.setTotalAmount(invoice.getTotalAmount().add(amount));
                invoice.setDeleted(false);
        });

        invoiceRepository.saveAll(invoiceList);
    }
}
