package com.emmanuelfinance.creditcard.invoice.selectors;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceItemSelector {

    private final InvoiceItemRepository invoiceItemRepository;

    public List<InvoiceItem> getByTransactionId(UUID transactionId) {
        List<InvoiceItem> invoiceItemList = invoiceItemRepository.findByTransactionId(
                transactionId
        );

        return invoiceItemList;
    }
}
