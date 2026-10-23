package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountDocumentSelector {

    private final AccountDocumentRepository accountDocumentRepository;
    private final SecurityUtils securityUtils;

    // procura a conta pelo id, userId e deleted false
    public AccountDocument getByIdAndUserIdAndDeletedFalse(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        AccountDocument account = accountDocumentRepository.findByIdAndUserIdAndDeletedFalse(accountId, userId)
                .orElseThrow(() -> new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        return account;
    }
}
