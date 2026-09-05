package com.emmanuelfinance.shared.modules.transaction;

import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.transaction-card-service-url")
public class TransactionClientCacheService {

    private final TransactionClient transactionClient;

    @Cacheable(value = "transaction_summary", key = "#id")
    public TransactionSummaryDTO getTransactionSummaryDTO(UUID id) {
        log.info("Buscando transaction via transaction server (cache miss)", id);
        return transactionClient.getTransactionSummary(id);
    }
}
