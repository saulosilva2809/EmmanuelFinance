package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "account-server",
        url = "${application.config.account-service-url}",
        configuration = AccountErrorDecoder.class
)
@ConditionalOnProperty(name = "application.config.account-service-url")
public interface AccountClient {

    @GetMapping("internal/accounts/summary/{id}")
    AccountSummaryDTO getAccountSummary(@PathVariable UUID id);

    @GetMapping("/internal/accounts/internal-summary/{id}")
    AccountSummaryInternalDTO getAccountSummaryInternal(@PathVariable UUID id);

    @GetMapping("/internal/accounts/{accountId}/ownership")
    boolean checkAccountOwner(
            @PathVariable UUID accountId,
            @RequestParam UUID userId
    );
}
