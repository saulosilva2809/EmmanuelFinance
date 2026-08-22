package com.emmanuelfinance.shared.modules.category;

import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "category-server", url = "${application.config.category-service-url}")
@ConditionalOnProperty(name = "application.config.category-service-url")
public interface CategoryClient {

    @GetMapping("internal/categories/summary/{id}")
    CategorySummaryDTO getCategorySummary(@PathVariable UUID id);
}
