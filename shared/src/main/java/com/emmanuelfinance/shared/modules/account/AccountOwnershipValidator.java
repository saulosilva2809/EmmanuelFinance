package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.config.account-service-url")
public class AccountOwnershipValidator {

    private final AccountCache accountCache;
    private final AccountClient accountClient;
    private final SecurityUtils securityUtils;

    public void validate(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();
        Boolean isOwnerInCache = accountCache.isAccountOwnedByUser(accountId, userId);

        if (Boolean.TRUE.equals(isOwnerInCache)) {
            return;
        }

        if (Boolean.FALSE.equals(isOwnerInCache)) {
            throw new AccountNotFound();
        }

        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "HS256")
                .claim("sub", userId)
                .build();

        boolean isOwnerInDB = accountClient.checkAccountOwner(accountId, jwt);

        if (!isOwnerInDB) {
            throw new AccountNotFound();
        }

        accountCache.saveAccountOwner(accountId, userId);
    }
}
