package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
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

        boolean isOwnerInDB = accountClient.checkAccountOwner(accountId, userId);

        if (!isOwnerInDB) {
            throw new AccountNotFound();
        }

        accountCache.saveAccountOwner(accountId, userId);
    }
}
