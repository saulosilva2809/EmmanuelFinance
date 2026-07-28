package com.emmanuelfinance.category;

import com.emmanuelfinance.shared.clients.AccountClient;
import com.emmanuelfinance.shared.dto.AccountSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountClientCacheService {

    private final AccountClient accountClient;

    @Cacheable(value = "accounts", key = "#id")
    public AccountSummaryDTO getInternalAccountById(UUID id) {
        System.out.println("GET via Client");
        return accountClient.getInternalAccountById(id);
    }
}
