package com.emmanuelfinance.account;

import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountSelector {

    private final AccountRepository accountRepository;
    private final SecurityUtils securityUtils;

    public Account getAccountByIdAndUserId(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        return accountRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(() -> new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));
    }

    public Account getAccountByIdIncludingDeleted(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        Account account = accountRepository.findByIdAndUserIdIncludingDeleted(accountId, userId)
                .orElseThrow(() -> new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        return account;
    }

    public Account getAccountByIdInternal(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        return account;
    }
}
