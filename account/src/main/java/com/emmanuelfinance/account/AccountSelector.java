package com.emmanuelfinance.account;

import com.emmanuelfinance.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountSelector {

    private final AccountRepository accountRepository;
    private final SecurityUtils securityUtils;

    public Account getAccountByIdAndUserId(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();

        Account account = accountRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new AccountNotFound());

        return account;
    }

    public Account getAccountByIdIncludingDeleted(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        Account account = accountRepository.findByIdAndUserIdIncludingDeleted(accountId, userId)
                .orElseThrow(() -> new AccountNotFound());

        return account;
    }
}
