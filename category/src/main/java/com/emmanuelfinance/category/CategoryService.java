package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.category.exceptions.AccountNotFound;
import com.emmanuelfinance.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.cache.AccountCache;
import com.emmanuelfinance.shared.dto.AccountSummaryDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.exceptions.CategoryAlreadyExists;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AccountClientCacheService accountClientCacheService;
    private final CategoryMapper categoryMapper;
    private final AccountCache accountCache;
    private final SecurityUtils securityUtils;

    private Category getCategoryById(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
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
                data.getUpdatedAt()
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

    public void checkAccountExists(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        boolean isOwner = accountCache.isAccountOwnedByUser(accountId, userId);
        if (!isOwner) {
            throw new AccountNotFound();
        }
    }

    public ResponseCategoryDTO create(CreateCategoryDTO data) {
        UUID userId = securityUtils.getCurrentUserId();
        checkCategoryExists(data);
        checkAccountExists(data.accountId());

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

    public PageResponseDTO<ResponseCategoryDTO> list(CategoryFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Category> specification = CategorySpecification.withFilter(filters, userId);
        Page<Category> page = categoryRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseCategoryDTO> dtoPage = page.map(this::categoryAsDTO);

        return PageResponseDTO.from(dtoPage);
    }

    public ResponseCategoryDTO update(UUID id, UpdateCategoryDTO data) {
        Category category = getCategoryById(id);

        if (data.accountId() != null) {
            checkAccountExists(data.accountId());
        }

        categoryMapper.updateCategoryFromDTO(data, category);

        Category savedCategory = categoryRepository.save(category);
        return categoryAsDTO(savedCategory);
    }

    public void delete(UUID id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
