package com.emmanuelfinance.shared.modules.category;

import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClient;
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
@ConditionalOnProperty(name = "application.config.category-service-url")
public class CategoryClientCacheService {

    private final CategoryClient categoryClient;

    @Cacheable(value = "category_summary", key = "#id")
    public CategorySummaryDTO getCategorySummaryDTO(UUID id) {
        log.info("Buscando category via category server (cache miss)", id);
        return categoryClient.getCategorySummary(id);
    }
}
