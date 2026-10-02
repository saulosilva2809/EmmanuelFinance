package com.emmanuelfinance.creditcard.controllers;

import com.emmanuelfinance.creditcard.services.CreditCardInternalService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@WebMvcTest(CreditCardInternalController.class)
@WithMockUser
public class CreditCardInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private CreditCardInternalService creditCardInternalService;

    @Nested
    @DisplayName("Cenários do GET /internal/credit-card/summary/{id}")
    class SummaryTests {

        @Test
        @DisplayName("Deve retornar 200 OK com o resumo do cartão")
        void shouldReturn200WhenFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(creditCardInternalService.getCreditCardSummary(id))
                    .thenReturn(new CreditCardSummaryDTO(id, "Cartão C6", false));

            mockMvc.perform(get("/internal/credit-card/summary/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.name").value("Cartão C6"))
                    .andExpect(jsonPath("$.deleted").value(false));

            verify(creditCardInternalService, times(1)).getCreditCardSummary(id);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando o cartão não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(creditCardInternalService.getCreditCardSummary(id)).thenThrow(new CreditCardNotFound());

            mockMvc.perform(get("/internal/credit-card/summary/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o id não for UUID")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(get("/internal/credit-card/summary/{id}", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(creditCardInternalService);
        }
    }

    @Nested
    @DisplayName("Cenários do GET /internal/credit-card/internal-summary/{id}")
    class InternalSummaryTests {

        @Test
        @DisplayName("Deve retornar 200 OK com o resumo interno do cartão")
        void shouldReturn200WhenFound() throws Exception {
            UUID id = UUID.randomUUID();
            UUID accountId = UUID.randomUUID();
            when(creditCardInternalService.getCreditCardInternalSummary(id)).thenReturn(
                    new CreditCardInternalSummaryDTO(id, accountId, new BigDecimal("8000.00"), 24, 17)
            );

            mockMvc.perform(get("/internal/credit-card/internal-summary/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                    .andExpect(jsonPath("$.availableLimit").value(8000.00))
                    .andExpect(jsonPath("$.dueDate").value(24))
                    .andExpect(jsonPath("$.closingDate").value(17));

            verify(creditCardInternalService, times(1)).getCreditCardInternalSummary(id);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando o cartão não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(creditCardInternalService.getCreditCardInternalSummary(id)).thenThrow(new CreditCardNotFound());

            mockMvc.perform(get("/internal/credit-card/internal-summary/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o id não for UUID")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(get("/internal/credit-card/internal-summary/{id}", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(creditCardInternalService);
        }
    }
}
