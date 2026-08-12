package com.emmanuelfinance.account;

import com.emmanuelfinance.shared.modules.user.UserClient;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserClientCacheService {

    private final UserClient userClient;

    @Cacheable(value = "users", key = "#userId")
    public UserSummaryDTO getUserById(UUID userId) {
        return userClient.getUserById(userId);
    }
}