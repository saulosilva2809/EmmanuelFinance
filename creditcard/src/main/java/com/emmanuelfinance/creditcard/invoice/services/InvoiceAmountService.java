package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceAmountService {

    private final InvoiceRepository invoiceRepository;
    private final OutboxService outboxService;

    private void markZeroAmountInvoicesAsDeleted(List<Invoice> invoiceList) {
        invoiceList.stream()
                // filtra todas invoices que o valor é 0
                .filter(invoice -> invoice.getTotalAmount() != null && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) == 0)
                // marca como deletadas
                .forEach(invoice -> invoice.setDeleted(true));
    }

    // remove valor (valor total / parcelas) após deletar uma transação
    public void removeBalanceAfterDeletion(List<Invoice> invoiceList, BigDecimal amount) {
        invoiceList.stream()
                .forEach(invoice -> invoice.setTotalAmount(
                            invoice.getTotalAmount().subtract(amount)
                ));

        // se alguma invoice ficar com o valor 0 marca como deletada
        markZeroAmountInvoicesAsDeleted(invoiceList);

        invoiceRepository.saveAllAndFlush(invoiceList);

        invoiceList.forEach(invoice -> {
            // se alguma invoice foi deletada
            if (invoice.isDeleted()) {
                // evento de deleção de invoice
                outboxService.invoiceDeleted(invoice);
            // evento de invoice alterada
            } else {
                outboxService.invoiceChanged(invoice);
            }
        });
    }

    // adiciona o valor (valor total / parcelas) após retaurar uma transação
    public void addBalanceAfterRestore(List<Invoice> invoiceList, BigDecimal amount) {
        // percore uma lista com todas as invoices, adicione o valor e desmaca deletada
        invoiceList.forEach(invoice -> {
                invoice.setTotalAmount(invoice.getTotalAmount().add(amount));
                invoice.setDeleted(false);
        });

        invoiceRepository.saveAllAndFlush(invoiceList);
        invoiceList.forEach(outboxService::invoiceChanged);
    }
}
