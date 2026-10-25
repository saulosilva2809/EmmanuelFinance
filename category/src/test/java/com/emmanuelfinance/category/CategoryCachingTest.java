package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.query.CategoryDocument;
import com.emmanuelfinance.category.query.CategoryDocumentRepository;
import com.emmanuelfinance.category.query.CategoryDocumentSelector;
import com.emmanuelfinance.category.query.CategoryProjector;
import com.emmanuelfinance.category.query.CategoryQueryService;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// sobe só o suficiente do spring para o cache funcionar de verdade: as anotações só valem através do proxy
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class CategoryCachingTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @Import({CategoryQueryService.class, CategoryProjector.class})
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        SecurityUtils securityUtils() {
            return Mockito.mock(SecurityUtils.class);
        }

        @Bean
        CategoryDocumentSelector categoryDocumentSelector() {
            return Mockito.mock(CategoryDocumentSelector.class);
        }

        @Bean
        CategoryDocumentRepository categoryDocumentRepository() {
            return Mockito.mock(CategoryDocumentRepository.class);
        }

        @Bean
        MongoTemplate mongoTemplate() {
            return Mockito.mock(MongoTemplate.class);
        }

        @Bean
        CategoryMapper categoryMapper() {
            return Mockito.mock(CategoryMapper.class);
        }
    }

    @Autowired
    private CategoryQueryService categoryQueryService;

    @Autowired
    private CategoryProjector categoryProjector;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CategoryDocumentSelector categoryDocumentSelector;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CategoryMapper categoryMapper;

    private final UUID userId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);
    private final CategoryFiltersDTO filters = new CategoryFiltersDTO(null, null);

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        Mockito.reset(securityUtils, categoryDocumentSelector, mongoTemplate, categoryMapper);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(mongoTemplate.count(any(Query.class), eq(CategoryDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(CategoryDocument.class))).thenReturn(List.of());
    }

    @Test
    void shouldReadTheCategoryFromMongoOnlyOnceWhileItIsCached() {
        CategoryDocument category = newCategory();

        categoryQueryService.view(category.getId());
        categoryQueryService.view(category.getId());

        verify(categoryDocumentSelector, times(1)).getCategoryById(category.getId());
    }

    // a chave precisa ter o id da categoria: sem ele a segunda categoria recebia a resposta da primeira
    @Test
    void shouldKeepTheViewOfEachCategoryApart() {
        CategoryDocument first = newCategory();
        CategoryDocument second = newCategory();

        assertEquals(first.getId(), categoryQueryService.view(first.getId()).id());
        assertEquals(second.getId(), categoryQueryService.view(second.getId()).id());
    }

    @Test
    void shouldKeepTheViewOfEachUserApart() {
        CategoryDocument category = newCategory();

        categoryQueryService.view(category.getId());
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        categoryQueryService.view(category.getId());

        verify(categoryDocumentSelector, times(2)).getCategoryById(category.getId());
    }

    @Test
    void shouldSearchMongoOnlyOnceForTheSameList() {
        categoryQueryService.list(filters, pageable);
        categoryQueryService.list(filters, pageable);

        verify(mongoTemplate, times(1)).find(any(Query.class), eq(CategoryDocument.class));
    }

    @Test
    void shouldSearchAgainWhenTheFiltersOrThePageChange() {
        categoryQueryService.list(filters, pageable);
        categoryQueryService.list(new CategoryFiltersDTO("Salário", null), pageable);
        categoryQueryService.list(filters, PageRequest.of(1, 10));

        verify(mongoTemplate, times(3)).find(any(Query.class), eq(CategoryDocument.class));
    }

    @Test
    void shouldKeepTheActiveAndTheDeletedListsApart() {
        categoryQueryService.list(filters, pageable);
        categoryQueryService.listDeleted(filters, pageable);

        verify(mongoTemplate, times(2)).find(any(Query.class), eq(CategoryDocument.class));
    }

    @Test
    void shouldDropTheViewAndTheListsWhenTheSnapshotIsProjected() {
        CategoryDocument category = newCategory();
        CategoryDocument other = newCategory();
        categoryQueryService.view(category.getId());
        categoryQueryService.view(other.getId());
        categoryQueryService.list(filters, pageable);
        categoryQueryService.listDeleted(filters, pageable);

        categoryProjector.project(category);

        categoryQueryService.view(category.getId());
        categoryQueryService.view(other.getId());
        categoryQueryService.list(filters, pageable);
        categoryQueryService.listDeleted(filters, pageable);
        verify(categoryDocumentSelector, times(2)).getCategoryById(category.getId());
        verify(categoryDocumentSelector, times(1)).getCategoryById(other.getId());
        verify(mongoTemplate, times(4)).find(any(Query.class), eq(CategoryDocument.class));
    }

    // esses dois caches são enchidos pelo transaction, e só o category sabe quando a categoria mudou
    @Test
    void shouldDropTheSummariesThatOtherServicesKeepOfTheCategory() {
        CategoryDocument category = newCategory();
        CategoryDocument other = newCategory();
        cacheManager.getCache("category_summary").put(userId + ":" + category.getId(), "resumo");
        cacheManager.getCache("category_internal_summary").put(userId + ":" + category.getId(), "resumo interno");
        cacheManager.getCache("category_summary").put(userId + ":" + other.getId(), "resumo de outra categoria");

        categoryProjector.project(category);

        assertNull(cacheManager.getCache("category_summary").get(userId + ":" + category.getId()));
        assertNull(cacheManager.getCache("category_internal_summary").get(userId + ":" + category.getId()));
        assertNotNull(cacheManager.getCache("category_summary").get(userId + ":" + other.getId()));
    }

    // a categoria aparece no nome que o transaction mostra nas transações e nos itens de fatura, e ela muda pouco
    @Test
    void shouldDropTheResponsesOfOtherServicesThatShowTheCategory() {
        CategoryDocument category = newCategory();
        cacheManager.getCache("transaction_view").put("chave", "resposta");
        cacheManager.getCache("transaction_list").put("chave", "resposta");
        cacheManager.getCache("invoice_item_list").put("chave", "resposta");

        categoryProjector.project(category);

        assertNull(cacheManager.getCache("transaction_view").get("chave"));
        assertNull(cacheManager.getCache("transaction_list").get("chave"));
        assertNull(cacheManager.getCache("invoice_item_list").get("chave"));
    }

    private CategoryDocument newCategory() {
        CategoryDocument category = new CategoryDocument();
        category.setId(UUID.randomUUID());
        category.setUserId(userId);
        when(categoryDocumentSelector.getCategoryById(category.getId())).thenReturn(category);
        when(categoryMapper.toResponseDTO(category)).thenReturn(
                new ResponseCategoryDTO(category.getId(), "Salário", TypeEnum.INCOME, LocalDateTime.now(), null, false));
        return category;
    }
}
