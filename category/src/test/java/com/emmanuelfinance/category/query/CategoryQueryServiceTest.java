package com.emmanuelfinance.category.query;

import com.emmanuelfinance.category.CategoryMapper;
import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceTest {

    @Mock
    private CategoryDocumentSelector categoryDocumentSelector;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CategoryQueryService categoryQueryService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldReturnTheCategoryOfTheUser() {
        CategoryDocument document = newDocument();
        ResponseCategoryDTO expected = newResponse(document);
        when(categoryDocumentSelector.getCategoryById(document.getId())).thenReturn(document);
        when(categoryMapper.toResponseDTO(document)).thenReturn(expected);

        assertEquals(expected, categoryQueryService.view(document.getId()));
    }

    @Test
    void shouldPropagateNotFoundWhenTheCategoryDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(categoryDocumentSelector.getCategoryById(id)).thenThrow(new CategoryNotFound());

        assertThrows(CategoryNotFound.class, () -> categoryQueryService.view(id));
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void shouldReturnAPageOfCategories() {
        Pageable pageable = PageRequest.of(0, 10);
        CategoryDocument document = newDocument();
        ResponseCategoryDTO expected = newResponse(document);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(CategoryDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(CategoryDocument.class))).thenReturn(List.of(document));
        when(categoryMapper.toResponseDTO(document)).thenReturn(expected);

        PageResponseDTO<ResponseCategoryDTO> result =
                categoryQueryService.list(new CategoryFiltersDTO(null, null), pageable);

        assertEquals(1, result.content().size());
        assertEquals(expected, result.content().get(0));
        assertEquals(1, result.totalElements());
    }

    @Test
    void shouldReturnAnEmptyPageOfDeletedCategories() {
        Pageable pageable = PageRequest.of(0, 10);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(CategoryDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(CategoryDocument.class))).thenReturn(List.of());

        PageResponseDTO<ResponseCategoryDTO> result =
                categoryQueryService.listDeleted(new CategoryFiltersDTO(null, null), pageable);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    // a lista e a lista de apagadas usam a mesma busca, só muda o filtro de deleted
    @Test
    void shouldSearchDeletedCategoriesOnlyWhenListingTheDeletedOnes() {
        Pageable pageable = PageRequest.of(0, 10);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(CategoryDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(CategoryDocument.class))).thenReturn(List.of());

        categoryQueryService.listDeleted(new CategoryFiltersDTO(null, null), pageable);

        verify(mongoTemplate).count(argThat((Query query) ->
                Boolean.TRUE.equals(query.getQueryObject().get("deleted"))), eq(CategoryDocument.class));
    }

    private CategoryDocument newDocument() {
        CategoryDocument document = new CategoryDocument();
        document.setId(UUID.randomUUID());
        document.setUserId(userId);
        document.setName("Salário");
        document.setType(TypeTransactionEnum.INCOME);
        return document;
    }

    private ResponseCategoryDTO newResponse(CategoryDocument document) {
        return new ResponseCategoryDTO(document.getId(), document.getName(), document.getType(),
                LocalDateTime.now(), null, false);
    }
}
