package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceItemRepository;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceItemService {

    private final InvoiceItemRepository invoiceItemRepository;

    @Transactional
    public void createInvoiceItem(
            TransactionCreatedEvent event,
            UUID invoiceId,
            Integer installmentNumber,
            BigDecimal installmentValue
    ) {
        InvoiceItem invoiceItem = new InvoiceItem();
        invoiceItem.setUserId(event.userId());
        invoiceItem.setInvoiceId(invoiceId);
        invoiceItem.setTransactionId(event.transactionId());
        invoiceItem.setInstallmentNumber(installmentNumber);
        invoiceItem.setTotalInstallments(event.installmentsCount());
        invoiceItem.setAmount(installmentValue);

        invoiceItemRepository.save(invoiceItem);
    }
}
