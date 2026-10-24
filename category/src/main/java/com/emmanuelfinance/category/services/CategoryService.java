package com.emmanuelfinance.category.services;

import com.emmanuelfinance.category.*;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.category.outbox.OutboxService;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SecurityUtils securityUtils;
    private final CategorySelector categorySelector;
    private final CategoryValidatorService categoryValidatorService;
    private final OutboxService outboxService;

    @Transactional
    public ResponseCategoryDTO create(CreateCategoryDTO data) {
        UUID userId = securityUtils.getCurrentUserId();
        categoryValidatorService.checkCategoryExists(data);

        Category category = categoryMapper.toEntity(data);
        category.setUserId(userId);

        Category savedCategory = categoryRepository.saveAndFlush(category);
        outboxService.categoryChanged(savedCategory);
        return categoryMapper.toResponseDTO(savedCategory);
    }

    @Transactional()
    public ResponseCategoryDTO update(UUID id, UpdateCategoryDTO data) {
        Category category = categorySelector.getCategoryById(id);

        categoryMapper.updateCategoryFromDTO(data, category);

        Category savedCategory = categoryRepository.saveAndFlush(category);
        outboxService.categoryChanged(savedCategory);
        return categoryMapper.toResponseDTO(savedCategory);
    }

    @Transactional()
    public void delete(UUID id) {
        Category category = categorySelector.getCategoryById(id);
        categoryRepository.delete(category);
        outboxService.categoryDeleted(category);
    }

    @Transactional
    public void restore(UUID id) {
        Category category = categorySelector.getCategoryByIdIncludingDeleted(id);

        categoryValidatorService.verifyIsDeleted(category);

        category.setDeleted(false);
        categoryRepository.saveAndFlush(category);
        outboxService.categoryChanged(category);
    }
}
