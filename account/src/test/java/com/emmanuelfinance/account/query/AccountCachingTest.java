package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.shared.modules.user.UserClientCacheService;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// sobe só o suficiente do spring para o cache funcionar de verdade: as anotações só valem através do proxy
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class AccountCachingTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @Import({AccountQueryService.class, AccountProjector.class})
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
        AccountDocumentSelector accountDocumentSelector() {
            return Mockito.mock(AccountDocumentSelector.class);
        }

        @Bean
        AccountDocumentRepository accountDocumentRepository() {
            return Mockito.mock(AccountDocumentRepository.class);
        }

        @Bean
        MongoTemplate mongoTemplate() {
            return Mockito.mock(MongoTemplate.class);
        }

        @Bean
        AccountMapper accountMapper() {
            return Mockito.mock(AccountMapper.class);
        }

        // o mapper tem um campo injetado, e o spring o preenche mesmo no dublê
        @Bean
        UserClientCacheService userClientCacheService() {
            return Mockito.mock(UserClientCacheService.class);
        }
    }

    @Autowired
    private AccountQueryService accountQueryService;

    @Autowired
    private AccountProjector accountProjector;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private AccountDocumentSelector accountDocumentSelector;

    @Autowired
    private AccountDocumentRepository accountDocumentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AccountMapper accountMapper;

    private final UUID userId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);
    private final AccountFiltersDTO filters = new AccountFiltersDTO(null, null);

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        Mockito.reset(securityUtils, accountDocumentSelector, accountDocumentRepository, mongoTemplate, accountMapper);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(AccountDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(AccountDocument.class))).thenReturn(List.of());
    }

    @Test
    void shouldReadTheAccountFromMongoOnlyOnceWhileItIsCached() {
        AccountDocument document = newDocument(userId);
        when(accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(document.getId())).thenReturn(document);
        when(accountMapper.toResponseDTO(document)).thenReturn(newResponse(document));

        ResponseAccountDTO first = accountQueryService.view(document.getId());
        ResponseAccountDTO second = accountQueryService.view(document.getId());

        assertEquals(first, second);
        verify(accountDocumentSelector, times(1)).getByIdAndUserIdAndDeletedFalse(document.getId());
    }

    // se a chave não tivesse o dono, o segundo usuário receberia a conta do primeiro
    @Test
    void shouldKeepTheCachedAccountOfEachUserApart() {
        UUID otherUser = UUID.randomUUID();
        AccountDocument document = newDocument(userId);
        when(accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(document.getId())).thenReturn(document);
        when(accountMapper.toResponseDTO(document)).thenReturn(newResponse(document));

        accountQueryService.view(document.getId());
        when(securityUtils.getCurrentUserId()).thenReturn(otherUser);
        accountQueryService.view(document.getId());

        verify(accountDocumentSelector, times(2)).getByIdAndUserIdAndDeletedFalse(document.getId());
    }

    @Test
    void shouldNotCacheAnAccountThatWasNotFound() {
        UUID id = UUID.randomUUID();
        when(accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(id)).thenThrow(new IllegalStateException("nao achou"));

        assertThrows(IllegalStateException.class, () -> accountQueryService.view(id));
        assertThrows(IllegalStateException.class, () -> accountQueryService.view(id));

        verify(accountDocumentSelector, times(2)).getByIdAndUserIdAndDeletedFalse(id);
    }

    @Test
    void shouldSearchMongoOnlyOnceForTheSameList() {
        accountQueryService.list(filters, pageable);
        accountQueryService.list(filters, pageable);

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(AccountDocument.class));
    }

    @Test
    void shouldSearchAgainWhenTheFiltersOrThePageChange() {
        accountQueryService.list(filters, pageable);
        accountQueryService.list(new AccountFiltersDTO("Nubank", null), pageable);
        accountQueryService.list(filters, PageRequest.of(1, 10));

        verify(mongoTemplate, times(3)).find(any(Query.class), eq(AccountDocument.class));
    }

    // a lista de ativas e a de excluídas usam os mesmos filtros e a mesma página, mas respostas diferentes
    @Test
    void shouldKeepTheActiveAndTheDeletedListsApart() {
        accountQueryService.list(filters, pageable);
        accountQueryService.listDeleted(filters, pageable);

        verify(mongoTemplate, times(2)).find(any(Query.class), eq(AccountDocument.class));
    }

    @Test
    void shouldDropTheCachedAccountAndTheListsWhenTheSnapshotIsProjected() {
        AccountDocument document = newDocument(userId);
        when(accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(document.getId())).thenReturn(document);
        when(accountMapper.toResponseDTO(document)).thenReturn(newResponse(document));
        accountQueryService.view(document.getId());
        accountQueryService.list(filters, pageable);
        accountQueryService.listDeleted(filters, pageable);

        accountProjector.project(document);

        accountQueryService.view(document.getId());
        accountQueryService.list(filters, pageable);
        accountQueryService.listDeleted(filters, pageable);
        verify(accountDocumentSelector, times(2)).getByIdAndUserIdAndDeletedFalse(document.getId());
        verify(mongoTemplate, times(4)).find(any(Query.class), eq(AccountDocument.class));
    }

    @Test
    void shouldOnlyDropTheViewOfTheAccountThatChanged() {
        AccountDocument changed = newDocument(userId);
        AccountDocument untouched = newDocument(userId);
        when(accountDocumentSelector.getByIdAndUserIdAndDeletedFalse(any())).thenReturn(changed).thenReturn(untouched);
        when(accountMapper.toResponseDTO(any(AccountDocument.class))).thenReturn(newResponse(changed));
        accountQueryService.view(changed.getId());
        accountQueryService.view(untouched.getId());

        accountProjector.project(changed);

        accountQueryService.view(untouched.getId());
        verify(accountDocumentSelector, times(2)).getByIdAndUserIdAndDeletedFalse(any());
    }

    // esses dois caches são enchidos pelos outros serviços (creditcard, transaction) e só o account sabe quando a conta mudou
    @Test
    void shouldDropTheSummariesThatOtherServicesKeepOfTheAccount() {
        AccountDocument document = newDocument(userId);
        AccountDocument other = newDocument(userId);
        cacheManager.getCache("accounts").put(userId + ":" + document.getId(), "resumo");
        cacheManager.getCache("accounts-internal").put(userId + ":" + document.getId(), "resumo interno");
        cacheManager.getCache("accounts").put(userId + ":" + other.getId(), "resumo de outra conta");

        accountProjector.project(document);

        assertNull(cacheManager.getCache("accounts").get(userId + ":" + document.getId()));
        assertNull(cacheManager.getCache("accounts-internal").get(userId + ":" + document.getId()));
        assertNotNull(cacheManager.getCache("accounts").get(userId + ":" + other.getId()));
    }

    // o "é dono" que os outros serviços guardam deixa de valer quando a conta é excluída
    @Test
    void shouldDropTheOwnerAnswerKeptByOtherServicesOnlyForThatUserAndAccount() {
        AccountDocument document = newDocument(userId);
        AccountDocument other = newDocument(userId);
        cacheManager.getCache("account_owner").put(document.getId() + ":" + userId, true);
        cacheManager.getCache("account_owner").put(other.getId() + ":" + userId, true);

        accountProjector.project(document);

        assertNull(cacheManager.getCache("account_owner").get(document.getId() + ":" + userId));
        assertNotNull(cacheManager.getCache("account_owner").get(other.getId() + ":" + userId));
    }

    private AccountDocument newDocument(UUID owner) {
        AccountDocument document = new AccountDocument();
        document.setId(UUID.randomUUID());
        document.setUserId(owner);
        return document;
    }

    private ResponseAccountDTO newResponse(AccountDocument document) {
        return new ResponseAccountDTO(document.getId(), "Nubank", null, null, BigDecimal.TEN, BigDecimal.TEN,
                LocalDateTime.now(), null, false);
    }
}
