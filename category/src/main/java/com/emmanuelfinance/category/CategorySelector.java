package com.emmanuelfinance.category;

import com.emmanuelfinance.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategorySelector {

    private final CategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;

    public Category getCategoryById(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new CategoryNotFound());

        return category;
    }

    public Category getCategoryByIdIncludingDeleted(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserIdIncludingDeleted(id, userId)
                .orElseThrow(() -> new CategoryNotFound());

        return category;
    }
}
