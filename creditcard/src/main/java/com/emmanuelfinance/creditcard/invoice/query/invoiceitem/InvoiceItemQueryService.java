package com.emmanuelfinance.creditcard.invoice.query.invoiceitem;

import com.emmanuelfinance.creditcard.invoice.InvoiceMapper;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceItemDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocumentRepository;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.transaction.TransactionClientCacheService;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceItemQueryService {

    private final MongoTemplate mongoTemplate;
    private final SecurityUtils securityUtils;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final TransactionClientCacheService transactionClientCacheService;

    @Cacheable(value = "invoice_item_list", key = "'active:' + @securityUtils.currentUserId + ':' + #invoiceId + ':' + #pageable")
    public PageResponseDTO<ResponseInvoiceItemDTO> listByInvoiceId(UUID invoiceId, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        ResponseInvoiceSummaryDTO invoiceSummary = invoiceDocumentRepository
                .findByIdAndUserIdAndDeletedFalse(invoiceId, userId)
                .map(invoiceMapper::toSummaryDTO)
                .orElseThrow(() -> new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND));

        Query query = InvoiceItemSpecification.withFilter(userId, invoiceId);
        long total = mongoTemplate.count(query, InvoiceItemDocument.class);

        List<InvoiceItemDocument> documents = mongoTemplate.find(query.with(pageable), InvoiceItemDocument.class);
        Page<ResponseInvoiceItemDTO> page = new PageImpl<>(documents, pageable, total)
                .map(document -> new ResponseInvoiceItemDTO(
                        invoiceSummary,
                        transactionClientCacheService.getTransactionSummaryDTO(document.getTransactionId()),
                        document.getInstallmentNumber(),
                        document.getTotalInstallments(),
                        document.getAmount()
                ));

        return PageResponseDTO.from(page);
    }
}
