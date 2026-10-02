package com.emmanuelfinance.category.services;

import com.emmanuelfinance.category.Category;
import com.emmanuelfinance.category.CategoryRepository;
import com.emmanuelfinance.category.CategoryTestDataBuilder;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.exceptions.CategoryDomainException;
import com.emmanuelfinance.category.exceptions.CategoryErrorCode;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryValidatorServiceTest {

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryValidatorService categoryValidatorService;

    @Nested
    @DisplayName("Tests of checkCategoryExists method")
    class CheckCategoryExistsMethod {

        @Test
        @DisplayName("Deve lançar exceção quando a categoria já existir para o usuário")
        void shouldThrowExceptionWhenCategoryAlreadyExists() {
            UUID userId = UUID.randomUUID();
            CreateCategoryDTO dto = CategoryTestDataBuilder.createCategoryDTO();

            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId(
                    dto.name(),
                    dto.type(),
                    userId
            )).thenReturn(true);

            CategoryDomainException exception = assertThrows(CategoryDomainException.class, () -> {
                categoryValidatorService.checkCategoryExists(dto);
            });

            assertEquals(CategoryErrorCode.CATEGORY_ALREADY_EXISTS, exception.getErrorCode());
            verify(categoryRepository, times(1))
                    .existsByNameIgnoreCaseAndTypeAndUserId(dto.name(), dto.type(), userId);
        }

        @Test
        @DisplayName("Não deve lançar exceção quando a categoria não existir para o usuário")
        void shouldNotThrowExceptionWhenCategoryDoesNotExist() {
            UUID userId = UUID.randomUUID();
            CreateCategoryDTO dto = CategoryTestDataBuilder.createCategoryDTO();

            when(securityUtils.getCurrentUserId()).thenReturn(userId);
            when(categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId(
                    dto.name(),
                    dto.type(),
                    userId
            )).thenReturn(false);

            assertDoesNotThrow(() -> categoryValidatorService.checkCategoryExists(dto));

            verify(categoryRepository, times(1))
                    .existsByNameIgnoreCaseAndTypeAndUserId(dto.name(), dto.type(), userId);
        }
    }

    @Nested
    @DisplayName("Tests of verifyIsDeleted method")
    class VerifyIsDeletedMethod {

        @Test
        @DisplayName("Deve lançar exceção quando a categoria NÃO estiver deletada")
        void shouldThrowExceptionWhenCategoryIsNotDeleted() {
            Category activeCategory = mock(Category.class);
            when(activeCategory.isDeleted()).thenReturn(false);

            CategoryDomainException exception = assertThrows(CategoryDomainException.class, () -> {
                categoryValidatorService.verifyIsDeleted(activeCategory);
            });

            assertEquals(CategoryErrorCode.RESTORE_CATEGORY_NOT_DELETED, exception.getErrorCode());
        }

        @Test
        @DisplayName("Não deve lançar exceção quando a categoria estiver deletada")
        void shouldNotThrowExceptionWhenCategoryIsDeleted() {
            Category deletedCategory = mock(Category.class);
            when(deletedCategory.isDeleted()).thenReturn(true);

            assertDoesNotThrow(() -> categoryValidatorService.verifyIsDeleted(deletedCategory));
        }
    }
}