package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFIltersDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import com.emmanuelfinance.category.enums.TypeEnum;
import com.emmanuelfinance.category.exceptions.AccountNotFound;
import com.emmanuelfinance.category.exceptions.CategoryAlreadyExists;
import com.emmanuelfinance.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
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
import org.springframework.data.redis.core.StringRedisTemplate;

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

    @Mock
    private AccountClientCacheService accountClientCacheService;

    @Spy
    private CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private CategoryService categoryService;

    @Nested
    @DisplayName("Tests of create method")
    class CreateMethodTests {

        @Test
        void shouldCreateACategorySuccessfully() {
            CreateCategoryDTO inputDto = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(inputDto);
            AccountSummaryDTO mockAccountSummary = CategoryTestDataBuilder.accountSummaryDTO(inputDto.accountId());
            ResponseCategoryDTO expectedResponse = CategoryTestDataBuilder.responseCategoryDTO(categoryEntity, mockAccountSummary);

            when(redisTemplate.hasKey(any(String.class))).thenReturn(true);

            when(accountClientCacheService.getInternalAccountById(inputDto.accountId()))
                    .thenReturn(mockAccountSummary);

            when(categoryRepository.save(any(Category.class)))
                    .thenReturn(categoryEntity);

            ResponseCategoryDTO response = categoryService.create(inputDto);

            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            assertEquals(expectedResponse.name(), response.name());
            assertEquals(mockAccountSummary, response.account());

            verify(accountClientCacheService, times(1)).getInternalAccountById(inputDto.accountId());
            verify(categoryRepository, times(1)).save(any(Category.class));
        }

        @Test
        void shouldGiveErrorWhenCreatingTwoEqualCategories() {
            CreateCategoryDTO inputDto = CategoryTestDataBuilder.createCategoryDTO();

            when(categoryRepository.existsByNameIgnoreCaseAndType(any(String.class), any(TypeEnum.class))).thenReturn(true);

            assertThrows(CategoryAlreadyExists.class, () -> {
                categoryService.create(inputDto);
            });

            verify(categoryRepository, times(1)).existsByNameIgnoreCaseAndType(any(String.class), any(TypeEnum.class));
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        void shouldGiveErrorWhenAccountDoesNotExist() {
            CreateCategoryDTO inputDto = CategoryTestDataBuilder.createCategoryDTO();

            when(redisTemplate.hasKey(any(String.class))).thenReturn(false);

            assertThrows(AccountNotFound.class, () -> {
                categoryService.create(inputDto);
            });

            verify(redisTemplate, times(1)).hasKey(any(String.class));
            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    @Nested
    @DisplayName("Tests of view method")
    class ViewMethodTests {

        @Test
        void shouldReturnACategorySuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO);
            AccountSummaryDTO mockAccount = CategoryTestDataBuilder.accountSummaryDTO(categoryDTO.accountId());
            ResponseCategoryDTO expectedResponse = CategoryTestDataBuilder.responseCategoryDTO(
                    categoryEntity,
                    mockAccount
            );

            when(accountClientCacheService.getInternalAccountById(categoryDTO.accountId()))
                    .thenReturn(mockAccount);
            when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.of(categoryEntity));

            ResponseCategoryDTO response = categoryService.view(expectedResponse.id());

            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            assertEquals(expectedResponse.name(), response.name());

            verify(categoryRepository, times(1)).findById(expectedResponse.id());
        }
    }

    @Test
    void shouldGiveCategoryNotFoundError() {
        UUID categoryNotFoundId = UUID.randomUUID();

        when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(CategoryNotFound.class, () -> {
            categoryService.view(categoryNotFoundId);
        });

        verify(categoryRepository, times(1)).findById(any(UUID.class));
    }

    @Nested
    @DisplayName("Tests of list method")
    class ListMethodTests {

        @Test
        void shouldListTheAccountsSuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO);
            AccountSummaryDTO mockAccount = CategoryTestDataBuilder.accountSummaryDTO(categoryDTO.accountId());

            CategoryFIltersDTO filters = new CategoryFIltersDTO(
                    categoryDTO.accountId(),
                    "Salá",
                    TypeEnum.INCOME
            );
            Pageable pageable = PageRequest.of(0, 10);
            List<Category> categoryList = List.of(categoryEntity);
            Page<Category> categoryPage = new PageImpl<>(categoryList, pageable, categoryList.size());

            when(categoryRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(categoryPage);
            when(accountClientCacheService.getInternalAccountById(any(UUID.class)))
                    .thenReturn(mockAccount);

            PageResponseDTO<ResponseCategoryDTO> result = categoryService.list(filters, pageable);

            assertNotNull(result);
            assertNotNull(result.content());
            assertEquals(1, result.content().size());
            assertEquals(categoryEntity.getId(), result.content().get(0).id());
            assertEquals("Salário", result.content().get(0).name());

            verify(categoryRepository, times(1)).findAll(any(Specification.class), eq(pageable));
            verify(accountClientCacheService, times(1)).getInternalAccountById(any(UUID.class));
        }

        @Test
        void shouldReturnABlankPage() {
            CategoryFIltersDTO filters = new CategoryFIltersDTO(
                    null,
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
            verifyNoInteractions(accountClientCacheService);
        }
    }

    @Nested
    class UpdateMethodTests{

        @Test
        void shouldUpdateACategorySuccessfully() {

            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO);
            AccountSummaryDTO mockAccount = CategoryTestDataBuilder.accountSummaryDTO(categoryDTO.accountId());

            UpdateCategoryDTO updateDTO = new UpdateCategoryDTO(
                    null,
                    "Conta Atualizada",
                    TypeEnum.EXPENSE
            );

            when(accountClientCacheService.getInternalAccountById(categoryDTO.accountId()))
                    .thenReturn(mockAccount);
            when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.of(categoryEntity));
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ResponseCategoryDTO result = categoryService.update(categoryEntity.getId(), updateDTO);

            assertEquals(updateDTO.name(), result.name());
            assertEquals(updateDTO.type(), result.type());

            verify(categoryRepository, times(1)).findById(any(UUID.class));
            verify(categoryRepository, times(1)).save(any(Category.class));
        }
    }

    @Nested
    class DeleteMethodTests{

        @Test
        void shouldDeleteACategorySuccessfully() {
            CreateCategoryDTO categoryDTO = CategoryTestDataBuilder.createCategoryDTO();
            Category categoryEntity = CategoryTestDataBuilder.categoryEntity(categoryDTO);

            when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.of(categoryEntity));

            assertDoesNotThrow(() -> categoryService.delete(categoryEntity.getId()));

            verify(categoryRepository, times(1)).findById(any(UUID.class));
            verify(categoryRepository, times(1)).delete(any(Category.class));
        }
    }
}