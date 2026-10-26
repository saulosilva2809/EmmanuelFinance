package com.emmanuelfinance.category.services;

import com.emmanuelfinance.category.*;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.category.exceptions.CategoryDomainException;
import com.emmanuelfinance.category.exceptions.CategoryErrorCode;
import com.emmanuelfinance.category.outbox.OutboxService;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CategorySelector categorySelector;

    @Mock
    private CategoryValidatorService categoryValidatorService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CategoryService categoryService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Nested
    @DisplayName("Tests of create method")
    class CreateMethodTests {

        @Test
        void shouldGiveErrorWhenCreatingTwoEqualCategories() {
            CreateCategoryDTO inputDto = CategoryTestDataBuilder.createCategoryDTO();

            doThrow(new CategoryDomainException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS))
                    .when(categoryValidatorService).checkCategoryExists(inputDto);

            CategoryDomainException exception = assertThrows(CategoryDomainException.class, () -> {
                categoryService.create(inputDto);
            });

            assertEquals(CategoryErrorCode.CATEGORY_ALREADY_EXISTS, exception.getErrorCode());

            verify(categoryValidatorService, times(1)).checkCategoryExists(inputDto);
            verify(categoryRepository, never()).saveAndFlush(any(Category.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldCreateACategorySuccessfully() {
            CreateCategoryDTO inputDto = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(inputDto, userId, false);
            ResponseCategoryDTO expectedResponse = CategoryTestDataBuilder.responseCategoryDTO(categoryEntity);

            when(categoryRepository.saveAndFlush(any(Category.class)))
                    .thenReturn(categoryEntity);

            ResponseCategoryDTO response = categoryService.create(inputDto);

            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            assertEquals(expectedResponse.name(), response.name());

            verify(categoryValidatorService, times(1)).checkCategoryExists(inputDto);
            verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));
            verify(outboxService, times(1)).categoryChanged(categoryEntity);
        }

        @Test
        void shouldSaveTheCategoryForTheCurrentUser() {
            CreateCategoryDTO inputDto = CategoryTestDataBuilder.createCategoryDTO();
            when(categoryRepository.saveAndFlush(any(Category.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            categoryService.create(inputDto);

            verify(categoryRepository).saveAndFlush(argThat(category ->
                    userId.equals(category.getUserId())
                            && inputDto.name().equals(category.getName())
                            && inputDto.type() == category.getType()));
        }
    }

    @Nested
    class UpdateMethodTests {

        @Test
        void shouldGiveErrorWhenTryingToUpdateADeletedAccount() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(
                    categoryDTO,
                    userId,
                    true
            );

            UpdateCategoryDTO updateDTO = new UpdateCategoryDTO(
                    "Conta Atualizada",
                    TypeTransactionEnum.EXPENSE
            );

            when(categorySelector.getCategoryById(categoryEntity.getId()))
                    .thenThrow(new CategoryNotFound());

            assertThrows(CategoryNotFound.class, () -> {
                categoryService.update(categoryEntity.getId(), updateDTO);
            });

            verify(categorySelector, times(1)).getCategoryById(categoryEntity.getId());
            verify(categoryRepository, never()).saveAndFlush(any(Category.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldUpdateACategorySuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, false);

            UpdateCategoryDTO updateDTO = new UpdateCategoryDTO(
                    "Conta Atualizada",
                    TypeTransactionEnum.EXPENSE
            );

            when(categorySelector.getCategoryById(categoryEntity.getId()))
                    .thenReturn(categoryEntity);
            when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ResponseCategoryDTO result = categoryService.update(categoryEntity.getId(), updateDTO);

            assertEquals(updateDTO.name(), result.name());
            assertEquals(updateDTO.type(), result.type());

            verify(categorySelector, times(1)).getCategoryById(categoryEntity.getId());
            verify(categoryRepository, times(1)).saveAndFlush(any(Category.class));
            verify(outboxService, times(1)).categoryChanged(categoryEntity);
        }
    }

    @Nested
    class DeleteMethodTests {

        @Test
        void shouldErrorWhenTryingToDeleteAnAlreadyDeletedAccount() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, true);

            when(categorySelector.getCategoryById(categoryEntity.getId()))
                    .thenThrow(new CategoryNotFound());

            assertThrows(CategoryNotFound.class, () -> {
                categoryService.delete(categoryEntity.getId());
            });

            verify(categorySelector, times(1)).getCategoryById(categoryEntity.getId());
            verify(categoryRepository, never()).delete(categoryEntity);
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldDeleteACategorySuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, false);

            when(categorySelector.getCategoryById(categoryEntity.getId()))
                    .thenReturn(categoryEntity);

            assertDoesNotThrow(() -> categoryService.delete(categoryEntity.getId()));

            verify(categorySelector, times(1)).getCategoryById(categoryEntity.getId());
            verify(categoryRepository, times(1)).delete(categoryEntity);
            verify(outboxService, times(1)).categoryDeleted(categoryEntity);
            verify(outboxService, never()).categoryChanged(any(Category.class));
        }
    }

    @Nested
    @DisplayName("Tests of restore method")
    class RestoreMethodTests {

        @Test
        void shouldFailWhenTryingToRestoreNonexistentCategory() {
            UUID categoryNotFoundId = UUID.randomUUID();

            when(categorySelector.getCategoryByIdIncludingDeleted(categoryNotFoundId))
                    .thenThrow(new CategoryNotFound());

            assertThrows(CategoryNotFound.class, () -> {
                categoryService.restore(categoryNotFoundId);
            });

            verify(categorySelector, times(1)).getCategoryByIdIncludingDeleted(categoryNotFoundId);
            verify(categoryRepository, never()).saveAndFlush(any(Category.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldGiveErrorWhenTryingToRestoreNonDeletedCategory() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(
                    categoryDTO,
                    userId,
                    false
            );

            when(categorySelector.getCategoryByIdIncludingDeleted(categoryEntity.getId()))
                    .thenReturn(categoryEntity);
            doThrow(new CategoryDomainException(CategoryErrorCode.RESTORE_CATEGORY_NOT_DELETED))
                    .when(categoryValidatorService).verifyIsDeleted(categoryEntity);

            CategoryDomainException exception = assertThrows(CategoryDomainException.class, () -> {
                categoryService.restore(categoryEntity.getId());
            });

            assertEquals(CategoryErrorCode.RESTORE_CATEGORY_NOT_DELETED, exception.getErrorCode());

            verify(categorySelector, times(1)).getCategoryByIdIncludingDeleted(categoryEntity.getId());
            verify(categoryRepository, never()).saveAndFlush(any(Category.class));
            verifyNoInteractions(outboxService);
        }

        @Test
        void shouldRestoreACategorySuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(
                    categoryDTO,
                    userId,
                    true
            );

            when(categorySelector.getCategoryByIdIncludingDeleted(categoryEntity.getId()))
                    .thenReturn(categoryEntity);

            categoryService.restore(categoryEntity.getId());

            assertFalse(categoryEntity.isDeleted());
            verify(categoryValidatorService, times(1)).verifyIsDeleted(categoryEntity);
            verify(categorySelector, times(1)).getCategoryByIdIncludingDeleted(categoryEntity.getId());
            verify(categoryRepository, times(1)).saveAndFlush(categoryEntity);
            verify(outboxService, times(1)).categoryChanged(categoryEntity);
        }
    }
}
