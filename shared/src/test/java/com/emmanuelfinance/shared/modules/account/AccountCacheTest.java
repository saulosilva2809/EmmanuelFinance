package com.emmanuelfinance.shared.modules.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AccountCache accountCache;

    private final UUID accountId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    // sem prazo a chave ficava para sempre, mesmo se o aviso de exclusão se perdesse
    @Test
    void shouldSaveTheOwnerWithAnExpirationTime() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        accountCache.saveAccountOwner(accountId, userId);

        verify(valueOperations).set("account:" + accountId, userId.toString(), Duration.ofHours(24));
    }

    @Test
    void shouldDeleteTheOwner() {
        accountCache.deleteAccountOwner(accountId);

        verify(redisTemplate).delete("account:" + accountId);
    }

    @Test
    void shouldAnswerTrueWhenTheCachedOwnerIsTheUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("account:" + accountId)).thenReturn(userId.toString());

        assertTrue(accountCache.isAccountOwnedByUser(accountId, userId));
    }

    @Test
    void shouldAnswerFalseWhenTheCachedOwnerIsAnotherUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("account:" + accountId)).thenReturn(UUID.randomUUID().toString());

        assertFalse(accountCache.isAccountOwnedByUser(accountId, userId));
    }

    @Test
    void shouldAnswerNullWhenThereIsNothingInTheCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertNull(accountCache.isAccountOwnedByUser(accountId, userId));
    }
}
