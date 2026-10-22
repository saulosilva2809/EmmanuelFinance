package com.emmanuelfinance.transaction.query;

import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.dtos.TransactionFiltersDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceTest {

    @Mock
    private TransactionDocumentRepository transactionDocumentRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private TransactionQueryService transactionQueryService;

    private final UUID userId = UUID.randomUUID();

    private final TransactionFiltersDTO noFilters =
            new TransactionFiltersDTO(null, null, null, null, null, null, null, null, null);

    private ResponseTransactionDTO response(UUID id) {
        return new ResponseTransactionDTO(
                id, null, null, null, null, "Mercado", new BigDecimal("100.00"), 1, false,
                null, null, null, null, null, false
        );
    }

    @Test
    void shouldReturnTheTransactionOfTheUser() {
        TransactionDocument document = new TransactionDocument();
        document.setId(UUID.randomUUID());
        ResponseTransactionDTO expected = response(document.getId());
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(transactionDocumentRepository.findByIdAndUserIdAndDeletedFalse(document.getId(), userId))
                .thenReturn(Optional.of(document));
        when(transactionMapper.toResponseDTO(document)).thenReturn(expected);

        assertEquals(expected, transactionQueryService.view(document.getId()));
    }

    @Test
    void shouldThrowNotFoundWhenTheTransactionIsNotTheUsers() {
        UUID id = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(transactionDocumentRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFound.class, () -> transactionQueryService.view(id));
        verifyNoInteractions(transactionMapper);
    }

    @Test
    void shouldReturnAPageOfTransactions() {
        Pageable pageable = PageRequest.of(0, 10);
        TransactionDocument document = new TransactionDocument();
        document.setId(UUID.randomUUID());
        ResponseTransactionDTO expected = response(document.getId());
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(TransactionDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(TransactionDocument.class))).thenReturn(List.of(document));
        when(transactionMapper.toResponseDTO(document)).thenReturn(expected);

        PageResponseDTO<ResponseTransactionDTO> result = transactionQueryService.list(noFilters, pageable);

        assertEquals(List.of(expected), result.content());
        assertEquals(1, result.totalElements());
    }

    @Test
    void shouldReturnAnEmptyPageOfDeletedTransactions() {
        Pageable pageable = PageRequest.of(0, 10);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(TransactionDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(TransactionDocument.class))).thenReturn(List.of());

        PageResponseDTO<ResponseTransactionDTO> result = transactionQueryService.listDeleted(noFilters, pageable);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        verifyNoInteractions(transactionMapper);
    }
}
