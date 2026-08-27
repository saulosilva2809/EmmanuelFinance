package com.emmanuelfinance.shared.modules.creditcard;

import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.config.credit-card-service-url")
public class CreditCardClientCacheService {

    private final CreditCardClient creditCardClient;

    @Cacheable(value = "credit_card_summary", key = "#id")
    public CreditCardSummaryDTO getCreditCardSummaryDTO(UUID id) {
        log.info("Buscando credit-card via credit-card server (cache miss)", id);
        return creditCardClient.getCreditCardSummary(id);
    }

    @Cacheable(value = "credit_card_internal_summary", key = "#id")
    public CreditCardInternalSummaryDTO getCreditCardInternalSummaryDTO(UUID id) {
        log.info("Buscando credit-card via credit-card server (cache miss)", id);
        return creditCardClient.getCreditCardInternalSummary(id);
    }
}
