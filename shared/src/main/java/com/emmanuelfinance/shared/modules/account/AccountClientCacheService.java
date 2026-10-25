package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.config.account-service-url")
public class AccountClientCacheService {

    private final AccountClient accountClient;

    @Cacheable(value = "accounts", key = "@securityUtils.currentUserId + ':' + #id")
    public AccountSummaryDTO getAccountSummaryById(UUID id) {
        log.info("Buscando conta {} via account-server (cache miss)", id);
        return accountClient.getAccountSummary(id);
    }

    @Cacheable(value = "accounts-internal", key = "@securityUtils.currentUserId + ':' + #id")
    public AccountSummaryInternalDTO getInternalAccountById(UUID id) {
        log.info("Buscando conta {} via account-server (cache miss)", id);
        return accountClient.getAccountSummaryInternal(id);
    }

    // só o "sim" fica em cache: quem não é dono pergunta de novo, e a conta excluída sai do cache no projector do account
    @Cacheable(value = "account_owner", key = "#accountId + ':' + #userId", unless = "!#result")
    public boolean isAccountOwnedByUser(UUID accountId, UUID userId) {
        log.info("Conferindo o dono da conta {} via account-server (cache miss)", accountId);
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "HS256")
                .claim("sub", userId)
                .build();

        return accountClient.checkAccountOwner(accountId, jwt);
    }
}
