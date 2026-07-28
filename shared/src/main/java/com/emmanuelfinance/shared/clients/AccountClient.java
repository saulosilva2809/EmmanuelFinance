package com.emmanuelfinance.shared.clients;

import com.emmanuelfinance.shared.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "account-server", url = "${application.config.account-service-url}")
@ConditionalOnProperty(name = "application.config.account-service-url")
public interface AccountClient {

    @GetMapping("/accounts/internal/{id}")
    AccountSummaryDTO getInternalAccountById(@PathVariable UUID id);
}
