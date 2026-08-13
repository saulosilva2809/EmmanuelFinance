package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.config.account-service-url")
public class AccountClientCacheService {

    private final AccountClient accountClient;

    @Cacheable(value = "accounts", key = "#id")
    public AccountSummaryDTO getInternalAccountById(UUID id) {
        log.info("Buscando conta {} via account-server (cache miss)", id);
        return accountClient.getInternalAccountById(id);
    }
}
