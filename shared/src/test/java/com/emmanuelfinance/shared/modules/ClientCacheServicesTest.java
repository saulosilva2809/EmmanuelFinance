package com.emmanuelfinance.shared.modules;

import com.emmanuelfinance.shared.modules.account.AccountClient;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.emmanuelfinance.shared.modules.category.CategoryClient;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClient;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.TransactionClient;
import com.emmanuelfinance.shared.modules.transaction.TransactionClientCacheService;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

// os resumos que um serviço guarda do outro só podem ser reaproveitados pelo mesmo usuário: os dados por trás deles
// são filtrados pelo dono, e uma chave só com o id deixaria outro usuário ler a resposta guardada
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@TestPropertySource(properties = {
        "application.config.account-service-url=http://localhost",
        "application.config.category-service-url=http://localhost",
        "application.config.credit-card-service-url=http://localhost",
        "application.config.transaction-service-url=http://localhost"
})
class ClientCacheServicesTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @Import({AccountClientCacheService.class, CategoryClientCacheService.class,
            CreditCardClientCacheService.class, TransactionClientCacheService.class})
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
        AccountClient accountClient() {
            return Mockito.mock(AccountClient.class);
        }

        @Bean
        CategoryClient categoryClient() {
            return Mockito.mock(CategoryClient.class);
        }

        @Bean
        CreditCardClient creditCardClient() {
            return Mockito.mock(CreditCardClient.class);
        }

        @Bean
        TransactionClient transactionClient() {
            return Mockito.mock(TransactionClient.class);
        }
    }

    @Autowired
    private AccountClientCacheService accountService;

    @Autowired
    private CategoryClientCacheService categoryService;

    @Autowired
    private CreditCardClientCacheService creditCardService;

    @Autowired
    private TransactionClientCacheService transactionService;

    @Autowired
    private AccountClient accountClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private CreditCardClient creditCardClient;

    @Autowired
    private TransactionClient transactionClient;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CacheManager cacheManager;

    private final UUID id = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        Mockito.reset(accountClient, categoryClient, creditCardClient, transactionClient, securityUtils);
        when(securityUtils.getCurrentUserId()).thenReturn(userA);
        when(accountClient.getAccountSummary(id)).thenReturn(Mockito.mock(AccountSummaryDTO.class));
        when(accountClient.getAccountSummaryInternal(id)).thenReturn(Mockito.mock(AccountSummaryInternalDTO.class));
        when(categoryClient.getCategorySummary(id)).thenReturn(Mockito.mock(CategorySummaryDTO.class));
        when(categoryClient.getCategoryInternalSummary(id)).thenReturn(Mockito.mock(CategoryInternalSummaryDTO.class));
        when(creditCardClient.getCreditCardSummary(id)).thenReturn(Mockito.mock(CreditCardSummaryDTO.class));
        when(creditCardClient.getCreditCardInternalSummary(id)).thenReturn(Mockito.mock(CreditCardInternalSummaryDTO.class));
        when(transactionClient.getTransactionSummary(id)).thenReturn(Mockito.mock(TransactionSummaryDTO.class));
    }

    private void asUserB() {
        when(securityUtils.getCurrentUserId()).thenReturn(userB);
    }

    @Test
    void shouldKeepTheAccountSummaryOfEachUserApart() {
        accountService.getAccountSummaryById(id);
        accountService.getAccountSummaryById(id);
        asUserB();
        accountService.getAccountSummaryById(id);

        verify(accountClient, times(2)).getAccountSummary(id);
    }

    @Test
    void shouldKeepTheInternalAccountSummaryOfEachUserApart() {
        accountService.getInternalAccountById(id);
        accountService.getInternalAccountById(id);
        asUserB();
        accountService.getInternalAccountById(id);

        verify(accountClient, times(2)).getAccountSummaryInternal(id);
    }

    @Test
    void shouldKeepTheCategorySummariesOfEachUserApart() {
        categoryService.getCategorySummaryDTO(id);
        categoryService.getCategorySummaryDTO(id);
        categoryService.getCategoryInternalSummaryDTO(id);
        categoryService.getCategoryInternalSummaryDTO(id);
        asUserB();
        categoryService.getCategorySummaryDTO(id);
        categoryService.getCategoryInternalSummaryDTO(id);

        verify(categoryClient, times(2)).getCategorySummary(id);
        verify(categoryClient, times(2)).getCategoryInternalSummary(id);
    }

    @Test
    void shouldKeepTheCreditCardSummariesOfEachUserApart() {
        creditCardService.getCreditCardSummaryDTO(id);
        creditCardService.getCreditCardSummaryDTO(id);
        creditCardService.getCreditCardInternalSummaryDTO(id);
        creditCardService.getCreditCardInternalSummaryDTO(id);
        asUserB();
        creditCardService.getCreditCardSummaryDTO(id);
        creditCardService.getCreditCardInternalSummaryDTO(id);

        verify(creditCardClient, times(2)).getCreditCardSummary(id);
        verify(creditCardClient, times(2)).getCreditCardInternalSummary(id);
    }

    @Test
    void shouldKeepTheTransactionSummaryOfEachUserApart() {
        transactionService.getTransactionSummaryDTO(id);
        transactionService.getTransactionSummaryDTO(id);
        asUserB();
        transactionService.getTransactionSummaryDTO(id);

        verify(transactionClient, times(2)).getTransactionSummary(id);
    }
}
