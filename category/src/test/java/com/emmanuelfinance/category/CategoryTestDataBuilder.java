package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.shared.enums.TypeEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public class CategoryTestDataBuilder {

    public static CreateCategoryDTO createCategoryDTO() {
        return new CreateCategoryDTO(
                "Salário",
                TypeEnum.INCOME
        );
    }

    public static Category categoryEntity(CreateCategoryDTO inputDto, UUID userId, boolean isDeleted) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setUserId(userId);
        category.setName(inputDto.name());
        category.setType(inputDto.type());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(null);
        category.setDeleted(isDeleted);
        return category;
    }

    public static ResponseCategoryDTO responseCategoryDTO(Category category) {
        return new ResponseCategoryDTO(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.isDeleted()
        );
    }
}
