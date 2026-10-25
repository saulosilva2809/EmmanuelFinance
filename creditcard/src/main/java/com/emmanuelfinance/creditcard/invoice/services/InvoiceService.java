package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceItemSelector;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceSelector invoiceSelector;
    private final CreditCardSelector creditCardSelector;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemService invoiceItemService;
    private final InvoiceItemSelector invoiceItemSelector;
    private final InvoiceAmountService invoiceAmountService;
    private final OutboxService outboxService;

    @Transactional
    private Invoice createInvoice(TransactionCreatedEvent event, LocalDate targetDate, BigDecimal installmentAmount) {
        // vem do evento do kafka, que não tem usuário logado nem token: lê do banco daqui, sem chamar o próprio serviço por http
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());

        Invoice invoice = new Invoice();
        invoice.setUserId(event.userId());
        invoice.setCreditCardId(event.creditCardId());
        invoice.setMonth(targetDate.getMonthValue());
        invoice.setYear(targetDate.getYear());

        invoice.setDueDate(creditCard.getDueDay());
        invoice.setClosingDate(creditCard.getClosingDay());

        invoice.setTotalAmount(installmentAmount);
        invoice.setStatus(InvoiceStatusEnum.OPEN);

        invoiceRepository.save(invoice);
        outboxService.invoiceChanged(invoice);
        log.info("Criada nova INVOICE para {}/{}", targetDate.getMonthValue(), targetDate.getYear());

        return invoice;
    }

    @Transactional
    private Invoice processInvoiceForDate(TransactionCreatedEvent event, LocalDate targetDate, BigDecimal installmentAmount) {
        Optional<Invoice> invoiceOptional = invoiceSelector.getByCreditCardAndMonthAndYear(
                event.creditCardId(),
                targetDate.getMonthValue(),
                targetDate.getYear()
        );

        if (invoiceOptional.isEmpty()) {
            return createInvoice(event, targetDate, installmentAmount);
        } else {
            Invoice invoice = invoiceOptional.get();
            BigDecimal currentAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
            invoice.setTotalAmount(currentAmount.add(installmentAmount));

            invoiceRepository.saveAndFlush(invoice);
            outboxService.invoiceChanged(invoice);
            log.info("INVOICE já existe para {}/{}. Valor atualizado.", targetDate.getMonthValue(), targetDate.getYear());

            return invoice;
        }
    }

    @Transactional
    public void findOrCreate(TransactionCreatedEvent event) {
        int totalInstallments = event.installmentsCount() != null ? event.installmentsCount() : 1;

        BigDecimal installmentAmount = event.amount().divide(
                BigDecimal.valueOf(totalInstallments),
                2,
                RoundingMode.HALF_EVEN
        );

        for (int i = 0; i < totalInstallments; i++) {
            LocalDate installmentDate = event.date().toLocalDate().plusMonths(i);

            Invoice invoice = processInvoiceForDate(event, installmentDate, installmentAmount);
            invoiceItemService.createInvoiceItem(event, invoice.getId(), i+1, installmentAmount);
        }
    }

    @Transactional
    public void delete(TransactionDeletedAndRestoreEvent event) {
        log.info("Deletando invoices da transação: {}", event.transactionId());

        List<InvoiceItem> invoiceItems = invoiceItemSelector.findByTransactionId(event.transactionId());

        List<UUID> invoiceIds = invoiceItems.stream()
                .map(InvoiceItem::getInvoiceId)
                .distinct()
                .toList();

        invoiceItemService.delete(event.transactionId());

        if (!invoiceIds.isEmpty()) {
            List<Invoice> invoiceList = invoiceRepository.findAllById(invoiceIds);

            invoiceAmountService.removeBalanceAfterDeletion(invoiceList, invoiceItems.get(0).getAmount());
        }
    }

    @Transactional
    public void restore(TransactionDeletedAndRestoreEvent event) {
        log.info("Restaurando invoices da transação: {}", event.transactionId());

        List<InvoiceItem> invoiceItems = invoiceItemSelector.findByTransactionId(event.transactionId());

        List<UUID> invoiceIds = invoiceItems.stream()
                .map(InvoiceItem::getInvoiceId)
                .distinct()
                .toList();

        invoiceItemService.restore(event.transactionId());

        if (!invoiceIds.isEmpty()) {
            List<Invoice> invoiceList = invoiceRepository.findAllById(invoiceIds);

            invoiceAmountService.addBalanceAfterRestore(invoiceList, invoiceItems.get(0).getAmount());
        }
    }
}
