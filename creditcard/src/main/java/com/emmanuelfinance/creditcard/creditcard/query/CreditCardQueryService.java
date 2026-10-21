package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.creditcard.creditcard.CreditCardMapper;
import com.emmanuelfinance.creditcard.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
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
public class CreditCardQueryService {

    private final CreditCardDocumentSelector creditCardDocumentSelector;
    private final CreditCardMapper creditCardMapper;
    private final SecurityUtils securityUtils;
    private final MongoTemplate mongoTemplate;

    public ResponseCreditCardDTO view(UUID creditCardId) {
        CreditCardDocument document = creditCardDocumentSelector.getByIdAndUserIdAndDeletedFalse(creditCardId);

        return creditCardMapper.toResponseDTO(document);
    }

    public PageResponseDTO<ResponseCreditCardDTO> list(CreditCardFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, false);
    }

    public PageResponseDTO<ResponseCreditCardDTO> listDeleted(CreditCardFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, true);
    }

    private PageResponseDTO<ResponseCreditCardDTO> search(CreditCardFiltersDTO filtersDTO, Pageable pageable, boolean deleted) {
        UUID userId = securityUtils.getCurrentUserId();

        Query query = CreditCardSpecification.withFilter(filtersDTO, userId, deleted);
        long total = mongoTemplate.count(query, CreditCardDocument.class);

        List<CreditCardDocument> documents = mongoTemplate.find(query.with(pageable), CreditCardDocument.class);
        Page<ResponseCreditCardDTO> page = new PageImpl<>(documents, pageable, total)
                .map(creditCardMapper::toResponseDTO);

        return PageResponseDTO.from(page);
    }
}
