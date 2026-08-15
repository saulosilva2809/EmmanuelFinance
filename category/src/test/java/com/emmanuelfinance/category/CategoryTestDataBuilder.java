package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public class CategoryTestDataBuilder {

    public static CreateCategoryDTO createCategoryDTO() {
        return new CreateCategoryDTO(
                UUID.randomUUID(),
                "Salário",
                TypeEnum.INCOME
        );
    }

    public static Category categoryEntity(CreateCategoryDTO inputDto) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setAccountId(inputDto.accountId());
        category.setName(inputDto.name());
        category.setType(inputDto.type());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(null);
        return category;
    }

    public static ResponseCategoryDTO responseCategoryDTO(Category category, AccountSummaryDTO accountSummary) {
        return new ResponseCategoryDTO(
                category.getId(),
                accountSummary,
                category.getName(),
                category.getType(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.isDeleted()
        );
    }

    public static AccountSummaryDTO accountSummaryDTO(UUID accountId) {
        return new AccountSummaryDTO(
                accountId,
                "Conta Corrente",
                false
        );
    }
}