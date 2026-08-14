package com.emmanuelfinance.shared.modules.account;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountCache {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_KEY_PREFIX = "account:";

    public void saveAccountOwner(UUID accountId, UUID userId) {
        String redisKey = REDIS_KEY_PREFIX + accountId;
        redisTemplate.opsForValue().set(redisKey, userId.toString());
    }

    public Boolean isAccountOwnedByUser(UUID accountId, UUID userId) {
        String redisKey = REDIS_KEY_PREFIX + accountId;
        String ownerIdInCache = redisTemplate.opsForValue().get(redisKey);

        if (ownerIdInCache == null) {
            return null;
        }

        return ownerIdInCache.equals(userId.toString());
    }
}