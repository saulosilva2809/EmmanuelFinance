package com.emmanuelfinance.shared.modules.transaction;

import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "transaction-server",
        url = "${application.config.transaction-service-url}",
        configuration = TransactionErrorDecoder.class
)
@ConditionalOnProperty(name = "{application.config.transaction-service-url}")
public interface TransactionClient {

    @GetMapping("internal/transaction/summary/{id}")
    TransactionSummaryDTO getTransactionSummary(@PathVariable UUID id);
}
