package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceSelector invoiceSelector;
    private final CreditCardClientCacheService creditCardClientCacheService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemService invoiceItemService;

    private CreditCardInternalSummaryDTO getCreditCardInternal(UUID creditCardId) {
        return creditCardClientCacheService.getCreditCardInternalSummaryDTO(creditCardId);
    }

    private ResponseInvoiceDTO invoiceAsDTO(Invoice invoice) {
        CreditCardSummaryDTO cardSummaryDTO = creditCardClientCacheService.getCreditCardSummaryDTO(
                invoice.getCreditCardId()
        );

        return new ResponseInvoiceDTO(
                cardSummaryDTO,
                invoice.getMonth(),
                invoice.getYear(),
                invoice.getDueDate(),
                invoice.getClosingDate(),
                invoice.getTotalAmount(),
                invoice.getStatus(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    @Transactional
    private Invoice createInvoice(TransactionCreatedEvent event, LocalDate targetDate, BigDecimal installmentAmount) {
        CreditCardInternalSummaryDTO creditCard = getCreditCardInternal(event.creditCardId());

        Invoice invoice = new Invoice();
        invoice.setUserId(event.userId());
        invoice.setCreditCardId(event.creditCardId());
        invoice.setMonth(targetDate.getMonthValue());
        invoice.setYear(targetDate.getYear());

        invoice.setDueDate(creditCard.dueDate());
        invoice.setClosingDate(creditCard.closingDate());

        invoice.setTotalAmount(installmentAmount);
        invoice.setStatus(InvoiceStatusEnum.OPEN);

        invoiceRepository.save(invoice);
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
            invoice.setTotalAmount(currentAmount.add(event.amount()));

            invoiceRepository.save(invoice);
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

            // cria a fatura
            Invoice invoice = processInvoiceForDate(event, installmentDate, installmentAmount);
            // cria o item da fatura
            invoiceItemService.createInvoiceItem(event, invoice.getId(), i+1, installmentAmount);
        }
    }
}
