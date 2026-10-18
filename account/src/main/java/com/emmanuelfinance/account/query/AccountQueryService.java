package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
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
public class AccountQueryService {

    private final AccountDocumentRepository accountDocumentRepository;
    private final MongoTemplate mongoTemplate;
    private final AccountMapper accountMapper;
    private final SecurityUtils securityUtils;

    public ResponseAccountDTO view(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();

        AccountDocument document = accountDocumentRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        return accountMapper.toResponseDTO(document);
    }

    public PageResponseDTO<ResponseAccountDTO> list(AccountFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, false);
    }

    public PageResponseDTO<ResponseAccountDTO> listDeleted(AccountFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, true);
    }

    private PageResponseDTO<ResponseAccountDTO> search(AccountFiltersDTO filters, Pageable pageable, boolean deleted) {
        UUID userId = securityUtils.getCurrentUserId();

        Query query = AccountSpecification.withFilter(filters, userId, deleted);

        // o count precisa vir antes do with(pageable), que adiciona skip/limit na query
        long total = mongoTemplate.count(query, AccountDocument.class);
        List<AccountDocument> documents = mongoTemplate.find(query.with(pageable), AccountDocument.class);

        Page<ResponseAccountDTO> page = new PageImpl<>(documents, pageable, total)
                .map(accountMapper::toResponseDTO);

        return PageResponseDTO.from(page);
    }
}
