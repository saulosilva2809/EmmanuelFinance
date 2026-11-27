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
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private Invoice createInvoice(
            TransactionCreatedEvent event,
            CreditCard creditCard,
            YearMonth target,
            BigDecimal installmentAmount
    ) {
        Invoice invoice = new Invoice();
        invoice.setUserId(event.userId());
        invoice.setCreditCardId(event.creditCardId());
        invoice.setMonth(target.getMonthValue());
        invoice.setYear(target.getYear());

        invoice.setDueDate(creditCard.getDueDay());
        invoice.setClosingDate(creditCard.getClosingDay());

        invoice.setTotalAmount(installmentAmount);
        invoice.setStatus(InvoiceStatusEnum.OPEN);

        invoiceRepository.save(invoice);
        outboxService.invoiceChanged(invoice);
        log.info("Criada nova INVOICE para {}", target);

        return invoice;
    }

    private Invoice processInvoiceForMonth(
            TransactionCreatedEvent event,
            CreditCard creditCard,
            YearMonth target,
            BigDecimal installmentAmount
    ) {
        Optional<Invoice> invoiceOptional = invoiceSelector.getByCreditCardAndMonthAndYear(
                event.creditCardId(),
                target.getMonthValue(),
                target.getYear()
        );

        if (invoiceOptional.isEmpty()) {
            return createInvoice(event, creditCard, target, installmentAmount);
        }

        Invoice invoice = invoiceOptional.get();
        BigDecimal currentAmount = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        // a fatura excluída por ter zerado é reaproveitada, porque só pode existir uma por mês
        invoice.setDeleted(false);
        invoice.setTotalAmount(currentAmount.add(installmentAmount));
        // uma compra nova deixa a fatura paga em aberto de novo
        invoice.setStatus(InvoiceStatusEnum.OPEN);

        invoiceRepository.saveAndFlush(invoice);
        outboxService.invoiceChanged(invoice);
        log.info("INVOICE já existe para {}. Valor atualizado.", target);

        return invoice;
    }

    @Transactional
    public void findOrCreate(TransactionCreatedEvent event) {
        int totalInstallments = event.installmentsCount() != null ? event.installmentsCount() : 1;

        // vem do evento do kafka, que não tem usuário logado nem token: lê do banco daqui, sem chamar o próprio serviço por http
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());

        BigDecimal installmentAmount = event.amount().divide(
                BigDecimal.valueOf(totalInstallments),
                2,
                RoundingMode.DOWN
        );
        // os centavos que sobram da divisão entram na primeira parcela, para a soma bater com a compra
        BigDecimal remainder = event.amount()
                .subtract(installmentAmount.multiply(BigDecimal.valueOf(totalInstallments)));

        // compra feita depois do fechamento só entra na fatura do mês seguinte
        LocalDate purchaseDate = event.date().toLocalDate();
        YearMonth firstMonth = YearMonth.from(purchaseDate);
        if (purchaseDate.getDayOfMonth() > creditCard.getClosingDay()) {
            firstMonth = firstMonth.plusMonths(1);
        }

        for (int i = 0; i < totalInstallments; i++) {
            BigDecimal amount = i == 0 ? installmentAmount.add(remainder) : installmentAmount;

            Invoice invoice = processInvoiceForMonth(event, creditCard, firstMonth.plusMonths(i), amount);
            invoiceItemService.createInvoiceItem(event, invoice.getId(), i+1, amount);
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

            invoiceAmountService.removeBalanceAfterDeletion(invoiceList, amountByInvoice(invoiceItems));
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

            invoiceAmountService.addBalanceAfterRestore(invoiceList, amountByInvoice(invoiceItems));
        }
    }

    // as parcelas podem ter valores diferentes, então cada fatura recebe a soma das suas
    private Map<UUID, BigDecimal> amountByInvoice(List<InvoiceItem> invoiceItems) {
        return invoiceItems.stream()
                .collect(Collectors.toMap(InvoiceItem::getInvoiceId, InvoiceItem::getAmount, BigDecimal::add));
    }
}
