package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.config.account-service-url")
public class AccountOwnershipValidator {

    private final AccountClientCacheService accountClientCacheService;
    private final SecurityUtils securityUtils;

    public void validate(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        if (!accountClientCacheService.isAccountOwnedByUser(accountId, userId)) {
            throw new AccountNotFound();
        }
    }
}
