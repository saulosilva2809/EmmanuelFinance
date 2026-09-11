package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceItemDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceItemRepository;
import com.emmanuelfinance.creditcard.invoice.specifications.InvoiceItemSpecification;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.transaction.TransactionClientCacheService;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final SecurityUtils securityUtils;
    private final InvoiceServiceInternal invoiceServiceInternal;
    private final TransactionClientCacheService transactionClientCacheService;

    private ResponseInvoiceItemDTO invoiceItemAsDTO(InvoiceItem invoiceItem) {
        ResponseInvoiceSummaryDTO invoiceSummaryDTO = invoiceServiceInternal.invoiceSummaryDTO(
                invoiceItem.getInvoiceId()
        );

        log.info("TRANSACTION ID: {}", invoiceItem.getTransactionId());
        TransactionSummaryDTO transactionSummaryDTO = transactionClientCacheService.getTransactionSummaryDTO(
                invoiceItem.getTransactionId()
        );


        return new ResponseInvoiceItemDTO(
                invoiceSummaryDTO,
                transactionSummaryDTO,
                invoiceItem.getInstallmentNumber(),
                invoiceItem.getTotalInstallments(),
                invoiceItem.getAmount()
        );
    }

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

    public PageResponseDTO<ResponseInvoiceItemDTO> listByInvoiceId(UUID invoiceId, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<InvoiceItem> specification = InvoiceItemSpecification.withFilter(userId, invoiceId);
        Page<InvoiceItem> page = invoiceItemRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseInvoiceItemDTO> dtoPage = page.map(this::invoiceItemAsDTO);
        return PageResponseDTO.from(dtoPage);
    }

    @Transactional
    public void delete(UUID transactionId) {
        log.info("Deletando InvoicesItems da transação: {}", transactionId);
        invoiceItemRepository.deleteByTransactionId(transactionId);
    }

    @Transactional
    public void restore(UUID transactionId) {
        log.info("Restaurando InvoicesItems da transação: {}", transactionId);
        List<InvoiceItem> invoiceItemList = invoiceItemRepository.findByTransactionId(transactionId);

        invoiceItemList.forEach(invoiceItem -> invoiceItem.setDeleted(false));
    }
}
