package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountOwnershipValidator {

    private final AccountCache accountCache;
    private final AccountClient accountClient;
    private final SecurityUtils securityUtils;

    public void validate(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        log.info("Validando posse da conta. AccountID: {}, UserID: {}", accountId, userId); // 👈 Adicione este log

        Boolean isOwnerInCache = accountCache.isAccountOwnedByUser(accountId, userId);

        if (Boolean.TRUE.equals(isOwnerInCache)) {
            return;
        }

        if (Boolean.FALSE.equals(isOwnerInCache)) {
            throw new AccountNotFound();
        }

        log.warn("Cache miss para accountId: {}. Consultando o Account-Service...", accountId);

        boolean isOwnerInDB = accountClient.checkAccountOwner(accountId, userId);

        log.info("Resultado da consulta no Account-Service para AccountID {}: {}", accountId, isOwnerInDB); // 👈 Adicione este log

        if (!isOwnerInDB) {
            throw new AccountNotFound();
        }

        accountCache.saveAccountOwner(accountId, userId);
    }
}
