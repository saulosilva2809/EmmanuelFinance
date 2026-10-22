package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceItemRepository;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceItemService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final OutboxService outboxService;

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
        outboxService.invoiceItemChanged(invoiceItem);
    }

    @Transactional
    public void delete(UUID transactionId) {
        log.info("Deletando InvoicesItems da transação: {}", transactionId);
        List<InvoiceItem> invoiceItemList = invoiceItemRepository.findByTransactionId(transactionId);

        invoiceItemRepository.deleteAll(invoiceItemList);
        invoiceItemList.forEach(outboxService::invoiceItemDeleted);
    }

    @Transactional
    public void restore(UUID transactionId) {
        log.info("Restaurando InvoicesItems da transação: {}", transactionId);
        List<InvoiceItem> invoiceItemList = invoiceItemRepository.findByTransactionId(transactionId);

        invoiceItemList.forEach(invoiceItem -> invoiceItem.setDeleted(false));
        invoiceItemRepository.saveAllAndFlush(invoiceItemList);
        invoiceItemList.forEach(outboxService::invoiceItemChanged);
    }
}
