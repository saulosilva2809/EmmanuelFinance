package com.emmanuelfinance.transaction.transaction.query;

import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.transaction.dtos.TransactionFiltersDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// lado de leitura: busca as transações no mongo e nunca no postgres. os dados chegam pelo outbox,
// então logo depois de uma escrita a leitura pode demorar um pouco para mostrar a mudança
@Service
@RequiredArgsConstructor
public class TransactionQueryService {

    private final TransactionDocumentRepository transactionDocumentRepository;
    private final TransactionMapper transactionMapper;
    private final SecurityUtils securityUtils;
    private final MongoTemplate mongoTemplate;

    public ResponseTransactionDTO view(UUID transactionId) {
        UUID userId = securityUtils.getCurrentUserId();

        TransactionDocument document = transactionDocumentRepository
                .findByIdAndUserIdAndDeletedFalse(transactionId, userId)
                .orElseThrow(TransactionNotFound::new);

        return transactionMapper.toResponseDTO(document);
    }

    public PageResponseDTO<ResponseTransactionDTO> list(TransactionFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, false);
    }

    public PageResponseDTO<ResponseTransactionDTO> listDeleted(TransactionFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, true);
    }

    // a lista de ativas e a de excluídas só mudam no filtro de excluído
    private PageResponseDTO<ResponseTransactionDTO> search(TransactionFiltersDTO filters, Pageable pageable, boolean deleted) {
        UUID userId = securityUtils.getCurrentUserId();

        Query query = TransactionSpecification.withFilter(filters, userId, deleted);
        // conta o total sem paginar, para saber quantas páginas existem
        long total = mongoTemplate.count(query, TransactionDocument.class);

        // busca só a página pedida, já com a ordem
        List<TransactionDocument> documents = mongoTemplate.find(query.with(pageable), TransactionDocument.class);
        Page<ResponseTransactionDTO> page = new PageImpl<>(documents, pageable, total)
                .map(transactionMapper::toResponseDTO);

        return PageResponseDTO.from(page);
    }
}
