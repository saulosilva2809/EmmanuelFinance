package com.emmanuelfinance.category.controllers;

import com.emmanuelfinance.category.services.CategoryInternalService;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.category.exceptions.CategoryNotFound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryInternalController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class CategoryInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private CategoryInternalService categoryInternalService;

    @Nested
    @DisplayName("Cenários do GET /internal/categories/summary/{id}")
    class GetCategorySummaryTests {

        @Test
        @DisplayName("Deve retornar 200 OK quando o resumo da categoria for encontrado")
        void shouldReturn200WhenCategorySummaryIsFound() throws Exception {
            UUID categoryId = UUID.randomUUID();
            CategorySummaryDTO summaryDTO = new CategorySummaryDTO(
                    categoryId,
                    "Categoria",
                    false
            );

            when(categoryInternalService.getCategorySummary(categoryId)).thenReturn(summaryDTO);

            mockMvc.perform(get("/internal/categories/summary/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(summaryDTO.id().toString()))
                    .andExpect(jsonPath("$.name").value(summaryDTO.name()));

            verify(categoryInternalService, times(1)).getCategorySummary(categoryId);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a categoria não for encontrada")
        void shouldReturn404WhenCategorySummaryIsNotFound() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            when(categoryInternalService.getCategorySummary(notFoundId))
                    .thenThrow(new CategoryNotFound());

            mockMvc.perform(get("/internal/categories/summary/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(categoryInternalService, times(1)).getCategorySummary(notFoundId);
        }
    }

    @Nested
    @DisplayName("Cenários do GET /internal/categories/internal-summary/{id}")
    class GetCategoryInternalSummaryTests {

        @Test
        @DisplayName("Deve retornar 200 OK quando o resumo interno da categoria for encontrado")
        void shouldReturn200WhenCategoryInternalSummaryIsFound() throws Exception {
            UUID categoryId = UUID.randomUUID();
            CategoryInternalSummaryDTO internalSummaryDTO = new CategoryInternalSummaryDTO(
                    categoryId,
                    TypeEnum.INCOME
            );

            when(categoryInternalService.getCategoryInternalSummary(categoryId)).thenReturn(internalSummaryDTO);

            mockMvc.perform(get("/internal/categories/internal-summary/{id}", categoryId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(internalSummaryDTO.id().toString()));

            verify(categoryInternalService, times(1)).getCategoryInternalSummary(categoryId);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a categoria não for encontrada")
        void shouldReturn404WhenCategoryInternalSummaryIsNotFound() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            when(categoryInternalService.getCategoryInternalSummary(notFoundId))
                    .thenThrow(new CategoryNotFound());

            mockMvc.perform(get("/internal/categories/internal-summary/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(categoryInternalService, times(1)).getCategoryInternalSummary(notFoundId);
        }
    }
}
