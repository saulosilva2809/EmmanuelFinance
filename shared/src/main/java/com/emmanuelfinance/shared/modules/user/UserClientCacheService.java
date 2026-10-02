package com.emmanuelfinance.shared.modules.user;

import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.config.user-service-url")
public class UserClientCacheService {

    private final UserClient userClient;

    @Cacheable(value = "users", key = "#userId")
    public UserSummaryDTO getUserById(UUID userId) {
        return userClient.getUserById(userId);
    }
}