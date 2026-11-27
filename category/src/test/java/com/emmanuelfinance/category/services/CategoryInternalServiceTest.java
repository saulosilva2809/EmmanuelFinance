package com.emmanuelfinance.category.services;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import com.emmanuelfinance.category.CategorySelector;
import com.emmanuelfinance.category.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CategoryInternalServiceTest {

    // quem decide (outros serviços chamando /internal) lê do Postgres. O Mongo é só para mostrar dados e pode
    // estar atrasado. Se isto falhar, alguém ligou o serviço interno ao lado de leitura.
    @Test
    @DisplayName("Não deve depender de nenhuma classe do lado de leitura (Mongo)")
    void shouldNotDependOnTheMongoReadModel() {
        boolean dependsOnReadModel = Arrays.stream(CategoryInternalService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().getPackageName().endsWith(".query"));

        assertFalse(dependsOnReadModel);
    }

    @Test
    @DisplayName("O resumo para exibição também devolve a categoria excluída")
    void shouldReturnTheSummaryOfADeletedCategory() {
        CategorySelector selector = mock(CategorySelector.class);
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Alimentação");
        category.setDeleted(true);
        when(selector.getCategoryByIdIncludingDeleted(category.getId())).thenReturn(category);

        assertTrue(new CategoryInternalService(selector).getCategorySummary(category.getId()).deleted());
    }
}
