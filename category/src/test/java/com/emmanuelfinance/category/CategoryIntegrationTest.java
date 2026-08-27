package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.services.CategoryService;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class CategoryIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private SecurityUtils securityUtils;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Nested
    @DisplayName("Tests of listDeleted method")
    class ListDeletedMethodTests {

        @Test
        void shouldListDeletedCards() {
            categoryRepository.deleteAll();

            CreateCategoryDTO categoryActiveDTO = new CreateCategoryDTO(
                    "categoria 1",
                    TypeEnum.EXPENSE
            );
            Category categoryActiveEntity = CategoryTestDataBuilder.categoryEntity(
                    categoryActiveDTO,
                    userId,
                    false
            );
            categoryActiveEntity.setUserId(userId);

            CreateCategoryDTO categoryDeletedDTO = new CreateCategoryDTO(
                    "categoria 2",
                    TypeEnum.EXPENSE
            );
            Category categoryDeletedEntity = CategoryTestDataBuilder.categoryEntity(
                    categoryDeletedDTO,
                    userId,
                    true
            );
            categoryDeletedEntity.setUserId(userId);

            CategoryFiltersDTO filters = new CategoryFiltersDTO(
                    null,
                    null
            );

            categoryRepository.save(categoryActiveEntity);
            Category savedDeletedCategory = categoryRepository.save(categoryDeletedEntity);

            Pageable pageable = PageRequest.of(0, 10);

            PageResponseDTO<ResponseCategoryDTO> response = categoryService.listDeleted(
                    filters,
                    pageable
            );

            assertEquals(1, response.content().size());
            assertEquals(savedDeletedCategory.getId(), response.content().get(0).id());
        }
    }

    @Nested
    @DisplayName("Tests of list method")
    class ListMethodTests {

        @Test
        void shouldListTheActiveCards() {
            categoryRepository.deleteAll();

            CreateCategoryDTO categoryActiveDTO = new CreateCategoryDTO(
                    "categoria 1",
                    TypeEnum.EXPENSE
            );
            Category categoryActiveEntity = CategoryTestDataBuilder.categoryEntity(
                    categoryActiveDTO,
                    userId,
                    false
            );
            categoryActiveEntity.setUserId(userId);

            CreateCategoryDTO categoryDeletedDTO = new CreateCategoryDTO(
                    "categoria 2",
                    TypeEnum.EXPENSE
            );
            Category categoryDeletedEntity = CategoryTestDataBuilder.categoryEntity(
                    categoryDeletedDTO,
                    userId,
                    true
            );
            categoryDeletedEntity.setUserId(userId);

            CategoryFiltersDTO filters = new CategoryFiltersDTO(
                    null,
                    null
            );

            categoryRepository.save(categoryDeletedEntity);
            Category savedActiveCategory = categoryRepository.save(categoryActiveEntity);

            Pageable pageable = PageRequest.of(0, 10);

            PageResponseDTO<ResponseCategoryDTO> response = categoryService.list(
                    filters,
                    pageable
            );

            assertEquals(1, response.content().size());
            assertEquals(savedActiveCategory.getId(), response.content().get(0).id());
        }
    }
}
