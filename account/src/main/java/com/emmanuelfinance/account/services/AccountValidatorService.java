package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountValidatorService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public boolean checkAccountOwner(UUID accountId, UUID userId) {
        return accountRepository.existsByIdAndUserIdAndDeletedFalse(accountId, userId);
    }
}
