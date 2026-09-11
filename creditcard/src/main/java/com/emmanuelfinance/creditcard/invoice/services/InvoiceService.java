package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceItemSelector;
import com.emmanuelfinance.creditcard.invoice.selectors.InvoiceSelector;
import com.emmanuelfinance.creditcard.invoice.specifications.InvoiceSpecification;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final CreditCardClientCacheService creditCardClientCacheService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemService invoiceItemService;
    private final InvoiceItemSelector invoiceItemSelector;
    private final SecurityUtils securityUtils;

    private CreditCardInternalSummaryDTO getCreditCardInternal(UUID creditCardId) {
        return creditCardClientCacheService.getCreditCardInternalSummaryDTO(creditCardId);
    }

    private ResponseInvoiceDTO invoiceAsDTO(Invoice invoice) {
        CreditCardSummaryDTO cardSummaryDTO = creditCardClientCacheService.getCreditCardSummaryDTO(
                invoice.getCreditCardId()
        );

        return new ResponseInvoiceDTO(
                invoice.getId(),
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

    public PageResponseDTO<ResponseInvoiceDTO> list(InvoiceFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Invoice> specification = InvoiceSpecification.withFilter(filters, userId);
        Page<Invoice> page = invoiceRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseInvoiceDTO> dtoPage = page.map(this::invoiceAsDTO);
        return PageResponseDTO.from(dtoPage);
    }

    @Transactional
    public void delete(TransactionDeletedAndRestoreEvent event) {
        log.info("Deletando invoices da transação: {}", event.transactionId());

        List<InvoiceItem> invoiceItems = invoiceItemSelector.getByTransactionId(event.transactionId());

        List<UUID> invoiceIds = invoiceItems.stream()
                .map(InvoiceItem::getInvoiceId)
                .distinct()
                .toList();

        invoiceItemService.delete(event.transactionId());

        if (!invoiceIds.isEmpty()) {
            invoiceRepository.deleteAllById(invoiceIds);
        }
    }

    @Transactional
    public void restore(TransactionDeletedAndRestoreEvent event) {
        log.info("Restaurando invoices da transação: {}", event.transactionId());

        List<InvoiceItem> invoiceItems = invoiceItemSelector.getByTransactionId(event.transactionId());

        List<UUID> invoiceIds = invoiceItems.stream()
                .map(InvoiceItem::getInvoiceId)
                .distinct()
                .toList();

        invoiceItemService.restore(event.transactionId());

        if (!invoiceIds.isEmpty()) {
            List<Invoice> invoiceList = invoiceRepository.findAllById(invoiceIds);

            invoiceList.forEach(invoice -> invoice.setDeleted(false));
        }
    }
}
