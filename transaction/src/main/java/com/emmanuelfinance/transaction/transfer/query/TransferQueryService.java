package com.emmanuelfinance.transaction.transfer.query;

import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.transfer.TransferMapper;
import com.emmanuelfinance.transaction.transfer.dtos.ResponseTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.TransferFiltersDTO;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferDomainException;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferErrorCode;
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
public class TransferQueryService {

    private final TransferDocumentRepository transferDocumentRepository;
    private final TransferMapper transferMapper;
    private final SecurityUtils securityUtils;
    private final MongoTemplate mongoTemplate;

    @Cacheable(value = "transfer_view", key = "@securityUtils.currentUserId + ':' + #transferId")
    public ResponseTransferDTO view(UUID transferId) {
        UUID userId = securityUtils.getCurrentUserId();

        TransferDocument transferDocument = transferDocumentRepository
                .findByIdAndUserIdAndDeletedFalse(transferId, userId)
                .orElseThrow(() -> new TransferDomainException(TransferErrorCode.TRANSFER_NOT_FOUND));

        return transferMapper.toResponseDTO(transferDocument);
    }

    @Cacheable(value = "transfer_list", key = "'active:' + @securityUtils.currentUserId + ':' + #filters + ':' + #pageable")
    public PageResponseDTO<ResponseTransferDTO> list(TransferFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, false);
    }

    @Cacheable(value = "transfer_list", key = "'deleted:' + @securityUtils.currentUserId + ':' + #filters + ':' + #pageable")
    public PageResponseDTO<ResponseTransferDTO> listDeleted(TransferFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, true);
    }

    private PageResponseDTO<ResponseTransferDTO> search(TransferFiltersDTO filters, Pageable pageable, boolean deleted) {
        UUID userId = securityUtils.getCurrentUserId();

        Query query = TransferSpecification.withFilter(filters, userId, deleted);
        long total = mongoTemplate.count(query, TransferDocument.class);

        List<TransferDocument> documents = mongoTemplate.find(query.with(pageable), TransferDocument.class);
        Page<ResponseTransferDTO> page = new PageImpl<>(documents, pageable, total)
                .map(transferMapper::toResponseDTO);

        return PageResponseDTO.from(page);
    }
}
