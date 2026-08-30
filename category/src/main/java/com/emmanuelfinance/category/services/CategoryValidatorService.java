package com.emmanuelfinance.category.services;

import com.emmanuelfinance.category.Category;
import com.emmanuelfinance.category.CategoryRepository;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.exceptions.CategoryAlreadyExists;
import com.emmanuelfinance.category.exceptions.RestoreCategoryError;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryValidatorService {

    private final SecurityUtils securityUtils;
    private final CategoryRepository categoryRepository;

    public void checkCategoryExists(CreateCategoryDTO data) {
        UUID userId = securityUtils.getCurrentUserId();
        boolean exists = categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId(
                data.name(),
                data.type(),
                userId
        );
        if (exists) {
            throw new CategoryAlreadyExists();
        }
    }

    public void verifyIsDeleted(Category category) {
        if (!category.isDeleted()) {
            throw new RestoreCategoryError();
        }
    }
}
