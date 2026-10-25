package com.emmanuelfinance.shared.modules.account;

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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// a anotação de cache só vale através do proxy do spring, então sobe só o suficiente para ele existir
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@TestPropertySource(properties = "application.config.account-service-url=http://localhost")
class AccountClientCacheServiceTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @Import(AccountClientCacheService.class)
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        AccountClient accountClient() {
            return Mockito.mock(AccountClient.class);
        }
    }

    @Autowired
    private AccountClientCacheService accountClientCacheService;

    @Autowired
    private AccountClient accountClient;

    @Autowired
    private CacheManager cacheManager;

    private final UUID accountId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        Mockito.reset(accountClient);
    }

    @Test
    void shouldAskTheAccountServiceOnlyOnceWhileTheOwnerIsCached() {
        when(accountClient.checkAccountOwner(eq(accountId), any(Jwt.class))).thenReturn(true);

        assertTrue(accountClientCacheService.isAccountOwnedByUser(accountId, userId));
        assertTrue(accountClientCacheService.isAccountOwnedByUser(accountId, userId));

        verify(accountClient, times(1)).checkAccountOwner(eq(accountId), any(Jwt.class));
    }

    // quem não é dono pergunta de novo: guardar o "não" faria uma conta recém-criada parecer alheia
    @Test
    void shouldNotKeepTheNegativeAnswer() {
        when(accountClient.checkAccountOwner(eq(accountId), any(Jwt.class))).thenReturn(false);

        assertFalse(accountClientCacheService.isAccountOwnedByUser(accountId, userId));
        assertFalse(accountClientCacheService.isAccountOwnedByUser(accountId, userId));

        verify(accountClient, times(2)).checkAccountOwner(eq(accountId), any(Jwt.class));
    }

    // a mesma conta consultada por outro usuário não pode aproveitar a resposta do dono
    @Test
    void shouldKeepTheAnswerOfEachUserApart() {
        UUID otherUser = UUID.randomUUID();
        when(accountClient.checkAccountOwner(eq(accountId), any(Jwt.class))).thenReturn(true).thenReturn(false);

        assertTrue(accountClientCacheService.isAccountOwnedByUser(accountId, userId));
        assertFalse(accountClientCacheService.isAccountOwnedByUser(accountId, otherUser));
    }

    @Test
    void shouldSendTheUserAsTheSubjectOfTheToken() {
        when(accountClient.checkAccountOwner(eq(accountId), any(Jwt.class))).thenReturn(true);

        accountClientCacheService.isAccountOwnedByUser(accountId, userId);

        verify(accountClient).checkAccountOwner(eq(accountId),
                Mockito.argThat(jwt -> userId.equals(jwt.getClaim("sub"))));
    }
}
