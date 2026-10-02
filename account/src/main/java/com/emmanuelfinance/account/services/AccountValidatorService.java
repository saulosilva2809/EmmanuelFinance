package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountValidatorService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public boolean checkAccountOwner(UUID accountId, Jwt jwt) {
        if (jwt == null) {
            return false;
        }
        String sub = jwt.getClaimAsString("sub");
        if (sub == null) {
            return false;
        }
        try {
            UUID userId = UUID.fromString(sub);
            return accountRepository.existsByIdAndUserIdAndDeletedFalse(accountId, userId);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
