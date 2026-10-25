package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.creditcard.CreditCardMapper;
import com.emmanuelfinance.creditcard.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocument;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocumentRepository;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardDocumentSelector;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardProjector;
import com.emmanuelfinance.creditcard.creditcard.query.CreditCardQueryService;
import com.emmanuelfinance.creditcard.invoice.InvoiceMapper;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceDocumentRepository;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceProjector;
import com.emmanuelfinance.creditcard.invoice.query.invoice.InvoiceQueryService;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemDocument;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemDocumentRepository;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemProjector;
import com.emmanuelfinance.creditcard.invoice.query.invoiceitem.InvoiceItemQueryService;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.transaction.TransactionClientCacheService;
import com.emmanuelfinance.shared.security.SecurityUtils;
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
class CreditCardCachingTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @Import({CreditCardQueryService.class, InvoiceQueryService.class, InvoiceItemQueryService.class,
            CreditCardProjector.class, InvoiceProjector.class, InvoiceItemProjector.class})
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
        CreditCardDocumentSelector creditCardDocumentSelector() {
            return Mockito.mock(CreditCardDocumentSelector.class);
        }

        @Bean
        CreditCardMapper creditCardMapper() {
            return Mockito.mock(CreditCardMapper.class);
        }

        // o mapper tem um campo injetado, e o spring o preenche mesmo no dublê
        @Bean
        AccountClientCacheService accountClientCacheService() {
            return Mockito.mock(AccountClientCacheService.class);
        }

        @Bean
        MongoTemplate mongoTemplate() {
            return Mockito.mock(MongoTemplate.class);
        }

        @Bean
        InvoiceMapper invoiceMapper() {
            return Mockito.mock(InvoiceMapper.class);
        }

        @Bean
        InvoiceDocumentRepository invoiceDocumentRepository() {
            return Mockito.mock(InvoiceDocumentRepository.class);
        }

        @Bean
        InvoiceItemDocumentRepository invoiceItemDocumentRepository() {
            return Mockito.mock(InvoiceItemDocumentRepository.class);
        }

        @Bean
        CreditCardDocumentRepository creditCardDocumentRepository() {
            return Mockito.mock(CreditCardDocumentRepository.class);
        }

        @Bean
        CreditCardClientCacheService creditCardClientCacheService() {
            return Mockito.mock(CreditCardClientCacheService.class);
        }

        @Bean
        TransactionClientCacheService transactionClientCacheService() {
            return Mockito.mock(TransactionClientCacheService.class);
        }
    }

    @Autowired
    private CreditCardQueryService creditCardQueryService;

    @Autowired
    private InvoiceQueryService invoiceQueryService;

    @Autowired
    private InvoiceItemQueryService invoiceItemQueryService;

    @Autowired
    private CreditCardProjector creditCardProjector;

    @Autowired
    private InvoiceProjector invoiceProjector;

    @Autowired
    private InvoiceItemProjector invoiceItemProjector;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CreditCardDocumentSelector creditCardDocumentSelector;

    @Autowired
    private CreditCardMapper creditCardMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Autowired
    private InvoiceDocumentRepository invoiceDocumentRepository;

    private final UUID userId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);
    private final CreditCardFiltersDTO cardFilters = new CreditCardFiltersDTO(null, null, null);
    private final InvoiceFiltersDTO invoiceFilters = new InvoiceFiltersDTO(null, null, null, null);

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        Mockito.reset(securityUtils, creditCardDocumentSelector, creditCardMapper, mongoTemplate, invoiceMapper,
                invoiceDocumentRepository);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(CreditCardDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(CreditCardDocument.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(InvoiceDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(InvoiceDocument.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(InvoiceItemDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(InvoiceItemDocument.class))).thenReturn(List.of());
    }

    @Test
    void shouldReadTheCardFromMongoOnlyOnceWhileItIsCached() {
        CreditCardDocument card = newCard();

        creditCardQueryService.view(card.getId());
        creditCardQueryService.view(card.getId());

        verify(creditCardDocumentSelector, times(1)).getByIdAndUserIdAndDeletedFalse(card.getId());
    }

    // a chave do cartão precisa ter o id do cartão: sem ele o segundo cartão recebia a resposta do primeiro
    @Test
    void shouldKeepTheViewOfEachCardApart() {
        CreditCardDocument first = newCard();
        CreditCardDocument second = newCard();

        ResponseCreditCardDTO responseOfFirst = creditCardQueryService.view(first.getId());
        ResponseCreditCardDTO responseOfSecond = creditCardQueryService.view(second.getId());

        assertEquals(first.getId(), responseOfFirst.id());
        assertEquals(second.getId(), responseOfSecond.id());
    }

    @Test
    void shouldKeepTheViewOfEachUserApart() {
        CreditCardDocument card = newCard();

        creditCardQueryService.view(card.getId());
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        creditCardQueryService.view(card.getId());

        verify(creditCardDocumentSelector, times(2)).getByIdAndUserIdAndDeletedFalse(card.getId());
    }

    @Test
    void shouldSearchMongoOnlyOnceForTheSameCardList() {
        creditCardQueryService.list(cardFilters, pageable);
        creditCardQueryService.list(cardFilters, pageable);

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(CreditCardDocument.class));
    }

    @Test
    void shouldKeepTheActiveAndTheDeletedCardListsApart() {
        creditCardQueryService.list(cardFilters, pageable);
        creditCardQueryService.listDeleted(cardFilters, pageable);

        verify(mongoTemplate, times(2)).find(any(Query.class), eq(CreditCardDocument.class));
    }

    @Test
    void shouldSearchMongoOnlyOnceForTheSameInvoiceList() {
        invoiceQueryService.list(invoiceFilters, pageable);
        invoiceQueryService.list(invoiceFilters, pageable);

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(InvoiceDocument.class));
    }

    @Test
    void shouldSearchMongoOnlyOnceForTheItemsOfTheSameInvoice() {
        UUID invoiceId = invoiceFound();

        invoiceItemQueryService.listByInvoiceId(invoiceId, pageable);
        invoiceItemQueryService.listByInvoiceId(invoiceId, pageable);

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(InvoiceItemDocument.class));
    }

    // a chave dos itens precisa ter o id da fatura: sem ele a segunda fatura recebia os itens da primeira
    @Test
    void shouldKeepTheItemsOfEachInvoiceApart() {
        UUID firstInvoice = invoiceFound();
        UUID secondInvoice = invoiceFound();

        invoiceItemQueryService.listByInvoiceId(firstInvoice, pageable);
        invoiceItemQueryService.listByInvoiceId(secondInvoice, pageable);

        verify(mongoTemplate, times(2)).find(any(Query.class), eq(InvoiceItemDocument.class));
    }

    @Test
    void shouldDropTheCardCachesAndTheInvoiceListWhenTheCardSnapshotIsProjected() {
        CreditCardDocument card = newCard();
        CreditCardDocument other = newCard();
        creditCardQueryService.view(card.getId());
        creditCardQueryService.view(other.getId());
        creditCardQueryService.list(cardFilters, pageable);
        creditCardQueryService.listDeleted(cardFilters, pageable);
        invoiceQueryService.list(invoiceFilters, pageable);
        cacheManager.getCache("credit_card_summary").put(userId + ":" + card.getId(), "resumo");
        cacheManager.getCache("credit_card_internal_summary").put(userId + ":" + card.getId(), "resumo interno");
        cacheManager.getCache("credit_card_summary").put(userId + ":" + other.getId(), "resumo de outro cartão");

        creditCardProjector.project(card);

        creditCardQueryService.view(card.getId());
        creditCardQueryService.view(other.getId());
        creditCardQueryService.list(cardFilters, pageable);
        creditCardQueryService.listDeleted(cardFilters, pageable);
        invoiceQueryService.list(invoiceFilters, pageable);
        verify(creditCardDocumentSelector, times(2)).getByIdAndUserIdAndDeletedFalse(card.getId());
        verify(creditCardDocumentSelector, times(1)).getByIdAndUserIdAndDeletedFalse(other.getId());
        verify(mongoTemplate, times(4)).find(any(Query.class), eq(CreditCardDocument.class));
        verify(mongoTemplate, times(2)).find(any(Query.class), eq(InvoiceDocument.class));
        assertNull(cacheManager.getCache("credit_card_summary").get(userId + ":" + card.getId()));
        assertNull(cacheManager.getCache("credit_card_internal_summary").get(userId + ":" + card.getId()));
        assertNotNull(cacheManager.getCache("credit_card_summary").get(userId + ":" + other.getId()));
    }

    // o cartão aparece no nome que as transações mostram, e as listas delas são do transaction
    @Test
    void shouldDropTheTransactionResponsesThatShowTheCardWhenTheCardSnapshotIsProjected() {
        CreditCardDocument card = newCard();
        cacheManager.getCache("transaction_view").put("chave", "resposta");
        cacheManager.getCache("transaction_list").put("chave", "resposta");

        creditCardProjector.project(card);

        assertNull(cacheManager.getCache("transaction_view").get("chave"));
        assertNull(cacheManager.getCache("transaction_list").get("chave"));
    }

    // a fatura muda o total e o status, e os itens trazem o resumo da fatura junto
    @Test
    void shouldDropTheInvoiceListAndTheItemListsWhenTheInvoiceSnapshotIsProjected() {
        UUID invoiceId = invoiceFound();
        invoiceQueryService.list(invoiceFilters, pageable);
        invoiceItemQueryService.listByInvoiceId(invoiceId, pageable);

        invoiceProjector.project(new InvoiceDocument());

        invoiceQueryService.list(invoiceFilters, pageable);
        invoiceItemQueryService.listByInvoiceId(invoiceId, pageable);
        verify(mongoTemplate, times(2)).find(any(Query.class), eq(InvoiceDocument.class));
        verify(mongoTemplate, times(2)).find(any(Query.class), eq(InvoiceItemDocument.class));
    }

    @Test
    void shouldDropOnlyTheItemListsWhenTheItemSnapshotIsProjected() {
        UUID invoiceId = invoiceFound();
        invoiceQueryService.list(invoiceFilters, pageable);
        invoiceItemQueryService.listByInvoiceId(invoiceId, pageable);

        invoiceItemProjector.project(new InvoiceItemDocument());

        invoiceQueryService.list(invoiceFilters, pageable);
        invoiceItemQueryService.listByInvoiceId(invoiceId, pageable);
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(InvoiceDocument.class));
        verify(mongoTemplate, times(2)).find(any(Query.class), eq(InvoiceItemDocument.class));
    }

    // o cartão precisa de id e dono para a chave do cache existir; o dublê devolve uma resposta com o id do cartão
    private CreditCardDocument newCard() {
        CreditCardDocument card = new CreditCardDocument();
        card.setId(UUID.randomUUID());
        card.setUserId(userId);
        when(creditCardDocumentSelector.getByIdAndUserIdAndDeletedFalse(card.getId())).thenReturn(card);
        when(creditCardMapper.toResponseDTO(card)).thenReturn(
                new ResponseCreditCardDTO(card.getId(), null, "Nubank", null, null, null, null, null, null, null, false));
        return card;
    }

    private UUID invoiceFound() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceDocumentRepository.findByIdAndUserIdAndDeletedFalse(invoiceId, userId))
                .thenReturn(Optional.of(new InvoiceDocument()));
        when(invoiceMapper.toSummaryDTO(any(InvoiceDocument.class))).thenReturn(Mockito.mock(ResponseInvoiceSummaryDTO.class));
        return invoiceId;
    }
}
