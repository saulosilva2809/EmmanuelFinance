package com.emmanuelfinance.category.services;

import com.emmanuelfinance.category.Category;
import com.emmanuelfinance.category.CategorySelector;
import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryInternalService {

    private final CategorySelector categorySelector;

    public CategorySummaryDTO getCategorySummary(UUID id) {
        // o resumo só mostra dados, então uma categoria excluída também precisa aparecer
        Category category = categorySelector.getCategoryByIdIncludingDeleted(id);
        return new CategorySummaryDTO(
                category.getId(),
                category.getName(),
                category.isDeleted()
        );
    }

    public CategoryInternalSummaryDTO getCategoryInternalSummary(UUID id) {
        Category category = categorySelector.getCategoryById(id);
        return new CategoryInternalSummaryDTO(
                category.getId(),
                category.getType()
        );
    }
}
