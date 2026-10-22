package com.emmanuelfinance.creditcard.invoice.query.invoice;

import com.emmanuelfinance.creditcard.invoice.InvoiceMapper;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
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
public class InvoiceQueryService {

    private final MongoTemplate mongoTemplate;
    private final SecurityUtils securityUtils;
    private final InvoiceMapper invoiceMapper;
    private final CreditCardClientCacheService creditCardClientCacheService;

    public PageResponseDTO<ResponseInvoiceDTO> list(InvoiceFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Query query = InvoiceSpecification.withFilter(filters, userId);
        long total = mongoTemplate.count(query, InvoiceDocument.class);

        List<InvoiceDocument> documents = mongoTemplate.find(query.with(pageable), InvoiceDocument.class);
        Page<ResponseInvoiceDTO> page = new PageImpl<>(documents, pageable, total)
                .map(document -> invoiceMapper.toResponseDTO(
                        document,
                        creditCardClientCacheService.getCreditCardSummaryDTO(document.getCreditCardId())
                ));

        return PageResponseDTO.from(page);
    }
}
