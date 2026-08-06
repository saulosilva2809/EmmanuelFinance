package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFIltersDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.category.exceptions.AccountNotFound;
import com.emmanuelfinance.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.dto.AccountSummaryDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.exceptions.CategoryAlreadyExists;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AccountClientCacheService accountClientCacheService;
    private final StringRedisTemplate redisTemplate;
    private final CategoryMapper categoryMapper;

    private Category getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
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
        boolean exists = categoryRepository.existsByNameIgnoreCaseAndType(data.name(), data.type());
        if (exists) {
            throw new CategoryAlreadyExists();
        }
    }

    private void checkAccountExists(CreateCategoryDTO data) {
        String redisKey = "account:exists:" + data.accountId();

        Boolean accountExists = redisTemplate.hasKey(redisKey);
        if (Boolean.FALSE.equals(accountExists)) {
            throw new AccountNotFound();
        }
    }

    public ResponseCategoryDTO create(CreateCategoryDTO data) {
        checkCategoryExists(data);
        checkAccountExists(data);

        Category category = new Category();

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

    public PageResponseDTO<ResponseCategoryDTO> list(CategoryFIltersDTO filters, Pageable pageable) {
        Specification<Category> specification = CategorySpecification.withFilter(filters);

        Page<Category> page = categoryRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseCategoryDTO> dtoPage = page.map(this::categoryAsDTO);

        return PageResponseDTO.from(dtoPage);
    }

    public ResponseCategoryDTO update(UUID id, UpdateCategoryDTO data) {
        Category category = getCategoryById(id);
        categoryMapper.updateCategoryFromDTO(data, category);

        Category savedCategory = categoryRepository.save(category);
        return categoryAsDTO(savedCategory);
    }

    public void delete(UUID id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
