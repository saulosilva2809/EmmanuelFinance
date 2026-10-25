package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.transaction.dtos.TransactionFiltersDTO;
import com.emmanuelfinance.transaction.transaction.query.TransactionDocument;
import com.emmanuelfinance.transaction.transaction.query.TransactionDocumentRepository;
import com.emmanuelfinance.transaction.transaction.query.TransactionProjector;
import com.emmanuelfinance.transaction.transaction.query.TransactionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// sobe só o suficiente do spring para o cache funcionar de verdade: as anotações só valem através do proxy
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class TransactionCachingTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @Import({TransactionQueryService.class, TransactionProjector.class})
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        SecurityUtils securityUtils() {
            return Mockito.mock(SecurityUtils.class);
        }

        @Bean
        TransactionDocumentRepository transactionDocumentRepository() {
            return Mockito.mock(TransactionDocumentRepository.class);
        }

        @Bean
        MongoTemplate mongoTemplate() {
            return Mockito.mock(MongoTemplate.class);
        }

        @Bean
        TransactionMapper transactionMapper() {
            return Mockito.mock(TransactionMapper.class);
        }

        // o mapper tem campos injetados, e o spring os preenche mesmo no dublê
        @Bean
        AccountClientCacheService accountClientCacheService() {
            return Mockito.mock(AccountClientCacheService.class);
        }

        @Bean
        CreditCardClientCacheService creditCardClientCacheService() {
            return Mockito.mock(CreditCardClientCacheService.class);
        }

        @Bean
        CategoryClientCacheService categoryClientCacheService() {
            return Mockito.mock(CategoryClientCacheService.class);
        }
    }

    @Autowired
    private TransactionQueryService transactionQueryService;

    @Autowired
    private TransactionProjector transactionProjector;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private TransactionDocumentRepository transactionDocumentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TransactionMapper transactionMapper;

    private final UUID userId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);
    private final TransactionFiltersDTO filters =
            new TransactionFiltersDTO(null, null, null, null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        Mockito.reset(securityUtils, transactionDocumentRepository, mongoTemplate, transactionMapper);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(TransactionDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(TransactionDocument.class))).thenReturn(List.of());
    }

    @Test
    void shouldReadTheTransactionFromMongoOnlyOnceWhileItIsCached() {
        TransactionDocument transaction = newTransaction();

        transactionQueryService.view(transaction.getId());
        transactionQueryService.view(transaction.getId());

        verify(transactionDocumentRepository, times(1)).findByIdAndUserIdAndDeletedFalse(transaction.getId(), userId);
    }

    // a chave precisa ter o id da transação: sem ele a segunda transação recebia a resposta da primeira
    @Test
    void shouldKeepTheViewOfEachTransactionApart() {
        TransactionDocument first = newTransaction();
        TransactionDocument second = newTransaction();

        ResponseTransactionDTO responseOfFirst = transactionQueryService.view(first.getId());
        ResponseTransactionDTO responseOfSecond = transactionQueryService.view(second.getId());

        assertSame(transactionMapper.toResponseDTO(first), responseOfFirst);
        assertSame(transactionMapper.toResponseDTO(second), responseOfSecond);
        assertNotSame(responseOfFirst, responseOfSecond);
    }

    @Test
    void shouldKeepTheViewOfEachUserApart() {
        TransactionDocument transaction = newTransaction();
        UUID otherUser = UUID.randomUUID();
        when(transactionDocumentRepository.findByIdAndUserIdAndDeletedFalse(transaction.getId(), otherUser))
                .thenReturn(Optional.of(transaction));

        transactionQueryService.view(transaction.getId());
        when(securityUtils.getCurrentUserId()).thenReturn(otherUser);
        transactionQueryService.view(transaction.getId());

        verify(transactionDocumentRepository, times(1)).findByIdAndUserIdAndDeletedFalse(transaction.getId(), userId);
        verify(transactionDocumentRepository, times(1)).findByIdAndUserIdAndDeletedFalse(transaction.getId(), otherUser);
    }

    @Test
    void shouldSearchMongoOnlyOnceForTheSameList() {
        transactionQueryService.list(filters, pageable);
        transactionQueryService.list(filters, pageable);

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(TransactionDocument.class));
    }

    @Test
    void shouldSearchAgainWhenTheFiltersOrThePageChange() {
        TransactionFiltersDTO otherFilters =
                new TransactionFiltersDTO(UUID.randomUUID(), null, null, null, null, null, null, null, null);

        transactionQueryService.list(filters, pageable);
        transactionQueryService.list(otherFilters, pageable);
        transactionQueryService.list(filters, PageRequest.of(1, 10));

        verify(mongoTemplate, times(3)).find(any(Query.class), eq(TransactionDocument.class));
    }

    @Test
    void shouldKeepTheActiveAndTheDeletedListsApart() {
        transactionQueryService.list(filters, pageable);
        transactionQueryService.listDeleted(filters, pageable);

        verify(mongoTemplate, times(2)).find(any(Query.class), eq(TransactionDocument.class));
    }

    @Test
    void shouldDropTheViewAndTheListsWhenTheSnapshotIsProjected() {
        TransactionDocument transaction = newTransaction();
        TransactionDocument other = newTransaction();
        transactionQueryService.view(transaction.getId());
        transactionQueryService.view(other.getId());
        transactionQueryService.list(filters, pageable);
        transactionQueryService.listDeleted(filters, pageable);

        transactionProjector.project(transaction);

        transactionQueryService.view(transaction.getId());
        transactionQueryService.view(other.getId());
        transactionQueryService.list(filters, pageable);
        transactionQueryService.listDeleted(filters, pageable);
        verify(transactionDocumentRepository, times(2)).findByIdAndUserIdAndDeletedFalse(transaction.getId(), userId);
        verify(transactionDocumentRepository, times(1)).findByIdAndUserIdAndDeletedFalse(other.getId(), userId);
        verify(mongoTemplate, times(4)).find(any(Query.class), eq(TransactionDocument.class));
    }

    // o creditcard guarda esse resumo para mostrar os itens da fatura, e só o transaction sabe quando a transação mudou
    @Test
    void shouldDropTheSummaryThatOtherServicesKeepOfTheTransaction() {
        TransactionDocument transaction = newTransaction();
        TransactionDocument other = newTransaction();
        cacheManager.getCache("transaction_summary").put(userId + ":" + transaction.getId(), "resumo");
        cacheManager.getCache("transaction_summary").put(userId + ":" + other.getId(), "resumo de outra transação");

        transactionProjector.project(transaction);

        assertNull(cacheManager.getCache("transaction_summary").get(userId + ":" + transaction.getId()));
        assertNotNull(cacheManager.getCache("transaction_summary").get(userId + ":" + other.getId()));
    }

    private TransactionDocument newTransaction() {
        TransactionDocument transaction = new TransactionDocument();
        transaction.setId(UUID.randomUUID());
        transaction.setUserId(userId);
        when(transactionDocumentRepository.findByIdAndUserIdAndDeletedFalse(transaction.getId(), userId))
                .thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponseDTO(transaction)).thenReturn(Mockito.mock(ResponseTransactionDTO.class));
        return transaction;
    }
}
