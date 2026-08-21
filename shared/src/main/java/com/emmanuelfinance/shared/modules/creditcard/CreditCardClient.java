package com.emmanuelfinance.shared.modules.creditcard;

import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "credit-credit-server", url = "${application.config.credit-card-service-url}")
@ConditionalOnProperty(name = "application.config.credit-card-service-url")
public interface CreditCardClient {

    @GetMapping("internal/credit-card/{id}")
    CreditCardSummaryDTO getCreditCardSummary(@PathVariable UUID id);
}
