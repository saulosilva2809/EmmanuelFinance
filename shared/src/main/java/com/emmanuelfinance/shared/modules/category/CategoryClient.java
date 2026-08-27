package com.emmanuelfinance.shared.modules.category;

import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "category-server",
        url = "${application.config.category-service-url}",
        configuration = CategoryErrorDecoder.class
)
@ConditionalOnProperty(name = "application.config.category-service-url")
public interface CategoryClient {

    @GetMapping("internal/categories/summary/{id}")
    CategorySummaryDTO getCategorySummary(@PathVariable UUID id);

    @GetMapping("internal/categories/internal-summary/{id}")
    CategoryInternalSummaryDTO getCategoryInternalSummary(@PathVariable UUID id);
}
