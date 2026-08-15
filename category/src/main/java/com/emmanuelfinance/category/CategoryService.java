package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.category.exceptions.RestoreCategoryError;
import com.emmanuelfinance.shared.annotation.WithDeletedFilter;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.exceptions.CategoryAlreadyExists;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AccountClientCacheService accountClientCacheService;
    private final CategoryMapper categoryMapper;
    private final SecurityUtils securityUtils;
    private final AccountOwnershipValidator accountOwnershipValidator;

    private Category getCategoryById(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new CategoryNotFound());

        return category;
    }

    private Category getCategoryByIdIncludingDeleted(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserIdIncludingDeleted(id, userId)
                .orElseThrow(() -> new CategoryNotFound());

        return category;
    }

    private ResponseCategoryDTO categoryAsDTO(Category data) {
        AccountSummaryDTO account = accountClientCacheService
                .getInternalAccountById(data.getAccountId());

        return new ResponseCategoryDTO(
                data.getId(),
                account,
                data.getName(),
                data.getType(),
                data.getCreatedAt(),
                data.getUpdatedAt(),
                data.isDeleted()
        );
    }

    private void checkCategoryExists(CreateCategoryDTO data) {
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

    public ResponseCategoryDTO create(CreateCategoryDTO data) {
        UUID userId = securityUtils.getCurrentUserId();
        checkCategoryExists(data);
        accountOwnershipValidator.validate(data.accountId());

        Category category = new Category();

        category.setUserId(userId);
        category.setAccountId(data.accountId());
        category.setName(data.name());
        category.setType(data.type());

        Category savedCategory = categoryRepository.save(category);
        return categoryAsDTO(savedCategory);
    }

    public ResponseCategoryDTO view(UUID id) {
        Category category = getCategoryById(id);
        return categoryAsDTO(category);
    }

    @Transactional(readOnly = true)
    @WithDeletedFilter(enabled = true)
    public PageResponseDTO<ResponseCategoryDTO> list(CategoryFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Category> specification = CategorySpecification.withFilter(filters, userId, false);
        Page<Category> page = categoryRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseCategoryDTO> dtoPage = page.map(this::categoryAsDTO);

        return PageResponseDTO.from(dtoPage);
    }

    @WithDeletedFilter(enabled = false)
    public PageResponseDTO<ResponseCategoryDTO> listDeleted(CategoryFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Category> specification = CategorySpecification.withFilter(filters, userId, true);
        Page<Category> page = categoryRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseCategoryDTO> dtoPage = page.map(this::categoryAsDTO);

        return PageResponseDTO.from(dtoPage);
    }

    @Transactional()
    public ResponseCategoryDTO update(UUID id, UpdateCategoryDTO data) {
        Category category = getCategoryById(id);

        if (data.accountId() != null) {
            accountOwnershipValidator.validate(data.accountId());
        }

        categoryMapper.updateCategoryFromDTO(data, category);

        Category savedCategory = categoryRepository.save(category);
        return categoryAsDTO(savedCategory);
    }

    @Transactional()
    public void delete(UUID id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }

    @Transactional
    public void restore(UUID id) {
        Category category = getCategoryByIdIncludingDeleted(id);

        if (!category.isDeleted()) {
            throw new RestoreCategoryError();
        }

        category.setDeleted(false);
        categoryRepository.save(category);
    }
}
