package com.emmanuelfinance.transaction.controllers;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.transaction.services.TransactionServiceInternal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(TransactionControllerInternal.class)
@WithMockUser
public class TransactionControllerInternalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private TransactionServiceInternal transactionServiceInternal;

    @Test
    @DisplayName("Deve retornar 200 OK com o resumo da transação")
    void shouldReturn200WithSummary() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(transactionServiceInternal.getSummaryDTO(transactionId)).thenReturn(new TransactionSummaryDTO(
                new CategorySummaryDTO(categoryId, "Alimentação", false),
                new BigDecimal("100.00"),
                StatusTransactionEnum.PAID,
                TypeEnum.EXPENSE
        ));

        mockMvc.perform(get("/internal/transactions/summary/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.category.name").value("Alimentação"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        verify(transactionServiceInternal, times(1)).getSummaryDTO(transactionId);
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a transação não existir")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(transactionServiceInternal.getSummaryDTO(transactionId)).thenThrow(new TransactionNotFound());

        mockMvc.perform(get("/internal/transactions/summary/{id}", transactionId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o id não for UUID")
    void shouldReturn400WhenIdIsInvalid() throws Exception {
        mockMvc.perform(get("/internal/transactions/summary/{id}", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionServiceInternal);
    }
}
