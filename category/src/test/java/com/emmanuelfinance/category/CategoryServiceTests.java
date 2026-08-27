package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.category.services.CategoryService;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.category.exceptions.CategoryAlreadyExists;
import com.emmanuelfinance.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.category.exceptions.RestoreCategoryError;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTests {

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AccountOwnershipValidator accountOwnershipValidator;

    @Mock
    private CategorySelector categorySelector;

    @InjectMocks
    private CategoryService categoryService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        userId = securityUtils.getCurrentUserId();
    }

    @Nested
    @DisplayName("Tests of create method")
    class CreateMethodTests {

        @Test
        void shouldGiveErrorWhenCreatingTwoEqualCategories() {
            CreateCategoryDTO inputDto = CategoryTestDataBuilder.createCategoryDTO();

            when(categoryRepository.existsByNameIgnoreCaseAndTypeAndUserId(
                    "Salário",
                    TypeEnum.INCOME,
                    userId
            )).thenReturn(true);

            assertThrows(CategoryAlreadyExists.class, () -> {
                categoryService.create(inputDto);
            });

            verify(categoryRepository, times(1)).existsByNameIgnoreCaseAndTypeAndUserId(
                    "Salário",
                    TypeEnum.INCOME,
                    userId
            );
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        void shouldCreateACategorySuccessfully() {
            CreateCategoryDTO inputDto = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(inputDto, userId, false);
            ResponseCategoryDTO expectedResponse = CategoryTestDataBuilder.responseCategoryDTO(categoryEntity);

            when(categoryRepository.save(any(Category.class)))
                    .thenReturn(categoryEntity);

            ResponseCategoryDTO response = categoryService.create(inputDto);

            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            assertEquals(expectedResponse.name(), response.name());

            verify(categoryRepository, times(1)).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("Tests of view method")
    class ViewMethodTests {

        @Test
        void shouldGiveCategoryNotFoundError() {
            UUID categoryNotFoundId = UUID.randomUUID();

            when(categorySelector.getCategoryById(categoryNotFoundId))
                    .thenThrow(new CategoryNotFound());

            assertThrows(CategoryNotFound.class, () -> {
                categoryService.view(categoryNotFoundId);
            });

            verify(categorySelector, times(1)).getCategoryById(categoryNotFoundId);
        }

        @Test
        void shouldReturnACategorySuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, false);
            ResponseCategoryDTO expectedResponse = CategoryTestDataBuilder.responseCategoryDTO(categoryEntity);

            when(categorySelector.getCategoryById(categoryEntity.getId()))
                    .thenReturn(categoryEntity);

            ResponseCategoryDTO response = categoryService.view(expectedResponse.id());

            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            assertEquals(expectedResponse.name(), response.name());

            verify(categorySelector, times(1)).getCategoryById(expectedResponse.id());
        }
    }

    @Nested
    @DisplayName("Tests of list method")
    class ListMethodTests {

        @Test
        void shouldReturnABlankPage() {
            CategoryFiltersDTO filters = new CategoryFiltersDTO(
                    null,
                    TypeEnum.EXPENSE
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> categoryPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(categoryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(categoryPage);

            PageResponseDTO<ResponseCategoryDTO> result = categoryService.list(filters, pageable);

            assertNotNull(result);
            assertTrue(result.content().isEmpty());

            verify(categoryRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        void shouldListTheAccountsSuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, false);

            CategoryFiltersDTO filters = new CategoryFiltersDTO(
                    "Salá",
                    TypeEnum.INCOME
            );
            Pageable pageable = PageRequest.of(0, 10);
            List<Category> categoryList = List.of(categoryEntity);
            Page<Category> categoryPage = new PageImpl<>(categoryList, pageable, categoryList.size());

            when(categoryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(categoryPage);

            PageResponseDTO<ResponseCategoryDTO> result = categoryService.list(filters, pageable);

            assertNotNull(result);
            assertNotNull(result.content());
            assertEquals(1, result.content().size());
            assertEquals(categoryEntity.getId(), result.content().get(0).id());
            assertEquals("Salário", result.content().get(0).name());

            verify(categoryRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Nested
    @DisplayName("Tests of listDeleted method")
    class ListDeletedMethodTests {

        @Test
        void shouldReturnABlankPage() {
            CategoryFiltersDTO filters = new CategoryFiltersDTO(
                    null,
                    TypeEnum.EXPENSE
            );
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> categoryPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(categoryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(categoryPage);

            PageResponseDTO<ResponseCategoryDTO> result = categoryService.listDeleted(filters, pageable);

            assertNotNull(result);
            assertTrue(result.content().isEmpty());

            verify(categoryRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        void shouldListTheAccountsSuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, true);

            CategoryFiltersDTO filters = new CategoryFiltersDTO(
                    "Salá",
                    TypeEnum.INCOME
            );
            Pageable pageable = PageRequest.of(0, 10);
            List<Category> categoryList = List.of(categoryEntity);
            Page<Category> categoryPage = new PageImpl<>(categoryList, pageable, categoryList.size());

            when(categoryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(categoryPage);

            PageResponseDTO<ResponseCategoryDTO> result = categoryService.listDeleted(filters, pageable);

            assertNotNull(result);
            assertNotNull(result.content());
            assertEquals(1, result.content().size());
            assertEquals(categoryEntity.getId(), result.content().get(0).id());
            assertEquals(categoryEntity.getName(), result.content().get(0).name());

            verify(categoryRepository, times(1)).findAll(any(Specification.class), eq(pageable));
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
                    TypeEnum.EXPENSE
            );

            when(categorySelector.getCategoryById(categoryEntity.getId()))
                    .thenThrow(new CategoryNotFound());

            assertThrows(CategoryNotFound.class, () -> {
                categoryService.update(categoryEntity.getId(), updateDTO);
            });

            verify(categorySelector, times(1)).getCategoryById(categoryEntity.getId());
            verify(categoryRepository, never()).save(categoryEntity);
        }

        @Test
        void shouldUpdateACategorySuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, false);

            UpdateCategoryDTO updateDTO = new UpdateCategoryDTO(
                    "Conta Atualizada",
                    TypeEnum.EXPENSE
            );

            when(categorySelector.getCategoryById(categoryEntity.getId()))
                    .thenReturn(categoryEntity);
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ResponseCategoryDTO result = categoryService.update(categoryEntity.getId(), updateDTO);

            assertEquals(updateDTO.name(), result.name());
            assertEquals(updateDTO.type(), result.type());

            verify(categorySelector, times(1)).getCategoryById(categoryEntity.getId());
            verify(categoryRepository, times(1)).save(any(Category.class));
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
        }

        @Test
        void shouldDeleteACategorySuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO, userId, false);

            when(categorySelector.getCategoryById(categoryEntity.getId()))
                    .thenReturn(categoryEntity);

            assertDoesNotThrow(() -> categoryService.delete(categoryEntity.getId()));

            verify(categorySelector, times(1)).getCategoryById(categoryEntity.getId());
            verify(categoryRepository, times(1)).delete(any(Category.class));
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
            verify(categoryRepository, never()).save(any(Category.class));
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

            assertThrows(RestoreCategoryError.class, () -> {
                categoryService.restore(categoryEntity.getId());
            });

            verify(categorySelector, times(1)).getCategoryByIdIncludingDeleted(categoryEntity.getId());
            verify(categoryRepository, never()).save(any(Category.class));
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

            verify(categorySelector, times(1)).getCategoryByIdIncludingDeleted(categoryEntity.getId());
            verify(categoryRepository, times(1)).save(any(Category.class));
        }
    }
}
