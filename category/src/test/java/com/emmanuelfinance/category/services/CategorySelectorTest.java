package com.emmanuelfinance.category.services;

import com.emmanuelfinance.category.Category;
import com.emmanuelfinance.category.CategoryRepository;
import com.emmanuelfinance.category.CategorySelector;
import com.emmanuelfinance.category.CategoryTestDataBuilder;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.shared.modules.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CategorySelectorTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private SecurityUtils securityUtils;

    private CategorySelector categorySelector;

    private UUID userId;
    private Category category;
    private Category categoryDeleted;

    @BeforeEach
    void setUp() {
        categorySelector = new CategorySelector(categoryRepository, securityUtils);

        userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
        CreateCategoryDTO categoryDT02 = CategoryTestDataBuilder.createCategoryDTO2();

        category = categoryRepository.save(CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, false));
        categoryDeleted = categoryRepository.save(CategoryTestDataBuilder.categoryEntity(categoryDT02, userId, true));
    }

    @Nested
    @DisplayName("Tests of getCategoryById method")
    class GetCategoryByIdMethod {

        @Test
        @DisplayName("Deve dar erro quando a categoria não for encontrada")
        void shouldGiveErrorWhenCategoryIsNotFound() {
            assertThrows(CategoryNotFound.class, () -> {
                categorySelector.getCategoryById(UUID.randomUUID());
            });
        }

        @Test
        @DisplayName("Deve dar erro quando a categoria for encontrada mas não for do usuário")
        void shouldReturnErrorWhenCategoryIsFoundButDoesNotBelongToUser() {
            UUID otherUserId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(otherUserId);

            assertThrows(CategoryNotFound.class, () -> {
                categorySelector.getCategoryById(category.getId());
            });
        }

        @Test
        @DisplayName("Deve dar erro ao tentar buscar uma categoria deletada")
        void shouldFailWhenTryingToFetchADeletedCategory() {
            assertThrows(CategoryNotFound.class, () -> {
                categorySelector.getCategoryById(categoryDeleted.getId());
            });
        }

        @Test
        @DisplayName("Deve retornar a categoria com sucesso")
        void shouldReturnTheCategorySuccessfully() {
            Category response = categorySelector.getCategoryById(category.getId());

            assertNotNull(response);
            assertEquals(category.getId(), response.getId());
            assertEquals(category.getName(), response.getName());
        }
    }

    @Nested
    @DisplayName("Tests of getCategoryByIdIncludingDeleted method")
    class GetCategoryByIdIncludingDeletedMethod {

        @Test
        @DisplayName("Deve dar erro quando a categoria não for encontrada")
        void shouldGiveErrorWhenCategoryIsNotFound() {
            assertThrows(CategoryNotFound.class, () -> {
                categorySelector.getCategoryByIdIncludingDeleted(UUID.randomUUID());
            });
        }

        @Test
        @DisplayName("Deve dar erro quando a categoria for encontrada mas não for do usuário")
        void shouldReturnErrorWhenCategoryIsFoundButDoesNotBelongToUser() {
            UUID otherUserId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(otherUserId);

            assertThrows(CategoryNotFound.class, () -> {
                categorySelector.getCategoryByIdIncludingDeleted(category.getId());
            });
        }

        @Test
        @DisplayName("Deve retornar a categoria deletada com sucesso")
        void shouldReturnTheDeletedCategorySuccessfully() {
            Category response = categorySelector.getCategoryByIdIncludingDeleted(categoryDeleted.getId());

            assertNotNull(response);
            assertEquals(categoryDeleted.getId(), response.getId());
            assertEquals(categoryDeleted.getName(), response.getName());
        }

        @Test
        @DisplayName("Deve retornar a categoria com sucesso")
        void shouldReturnTheCategorySuccessfully() {
            Category response = categorySelector.getCategoryByIdIncludingDeleted(category.getId());

            assertNotNull(response);
            assertEquals(category.getId(), response.getId());
            assertEquals(category.getName(), response.getName());
        }
    }
}