package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // tira de cada fatura o valor das parcelas da transação excluída
    public void removeBalanceAfterDeletion(List<Invoice> invoiceList, Map<UUID, BigDecimal> amountByInvoice) {
        invoiceList.forEach(invoice -> invoice.setTotalAmount(
                invoice.getTotalAmount().subtract(amountByInvoice.get(invoice.getId()))
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

    // devolve a cada fatura o valor das parcelas da transação restaurada
    public void addBalanceAfterRestore(List<Invoice> invoiceList, Map<UUID, BigDecimal> amountByInvoice) {
        // percore uma lista com todas as invoices, adicione o valor e desmaca deletada
        invoiceList.forEach(invoice -> {
                invoice.setTotalAmount(invoice.getTotalAmount().add(amountByInvoice.get(invoice.getId())));
                invoice.setDeleted(false);
        });

        invoiceRepository.saveAllAndFlush(invoiceList);
        invoiceList.forEach(outboxService::invoiceChanged);
    }
}
