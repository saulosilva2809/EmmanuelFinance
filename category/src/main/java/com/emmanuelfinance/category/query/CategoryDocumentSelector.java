package com.emmanuelfinance.category.query;

import com.emmanuelfinance.shared.modules.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryDocumentSelector {

    private final CategoryDocumentRepository categoryDocumentRepository;
    private final SecurityUtils securityUtils;

    public CategoryDocument getCategoryById(UUID categoryId) {
        UUID userId = securityUtils.getCurrentUserId();
        CategoryDocument category = categoryDocumentRepository.findByIdAndUserIdAndDeletedFalse(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFound());

        return category;
    }
}
