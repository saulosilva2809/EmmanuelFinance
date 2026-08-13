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
    private final SecurityUtils securityUtils;

    public void validate(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        boolean isOwner = accountCache.isAccountOwnedByUser(accountId, userId);
        if (!isOwner) {
            throw new AccountNotFound();
        }
    }
}
