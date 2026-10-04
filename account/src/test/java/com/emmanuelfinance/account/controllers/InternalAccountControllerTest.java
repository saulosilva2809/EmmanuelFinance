package com.emmanuelfinance.account.controllers;

import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.account.services.AccountInternalService;
import com.emmanuelfinance.account.services.AccountValidatorService;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@ActiveProfiles("test")
@WebMvcTest(InternalAccountController.class)
@WithMockUser
public class InternalAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountInternalService accountInternalService;

    @MockBean
    private AccountValidatorService accountValidatorService;

    @Nested
    @DisplayName("Cenários do GET /summary/{id}")
    class SummaryAccountTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os dados do resumo da conta quando o ID for encontrado")
        void shouldReturn200WhenAccountSummaryIsFound() throws Exception {
            UUID accountId = UUID.randomUUID();
            AccountSummaryDTO summaryDTO = new AccountSummaryDTO(
                    accountId,
                    "Conta Principal",
                    false
            );

            when(accountInternalService.getAccountSummary(accountId)).thenReturn(summaryDTO);

            mockMvc.perform(get("/internal/accounts/summary/{id}", accountId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(accountId.toString()))
                    .andExpect(jsonPath("$.name").value("Conta Principal"));

            verify(accountInternalService, times(1)).getAccountSummary(accountId);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a conta não for encontrada para o resumo")
        void shouldReturn404WhenAccountNotFoundForSummary() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            when(accountInternalService.getAccountSummary(notFoundId))
                    .thenThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            mockMvc.perform(get("/internal/accounts/summary/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(accountInternalService, times(1)).getAccountSummary(notFoundId);
        }
    }

    @Nested
    @DisplayName("Cenários do GET /internal-summary/{id}")
    class SummaryInternalAccountTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os dados do resumo interno da conta quando o ID for encontrado")
        void shouldReturn200WhenAccountSummaryInternalIsFound() throws Exception {
            UUID accountId = UUID.randomUUID();

            AccountSummaryInternalDTO summaryInternalDTO = new AccountSummaryInternalDTO(
                    accountId,
                    "Conta Secundária",
                    BanksEnum.NUBANK,
                    new BigDecimal("1500.00"),
                    false
            );

            when(accountInternalService.getAccountSummaryInternal(accountId)).thenReturn(summaryInternalDTO);

            mockMvc.perform(get("/internal/accounts/internal-summary/{id}", accountId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(accountId.toString()))
                    .andExpect(jsonPath("$.name").value("Conta Secundária"))
                    .andExpect(jsonPath("$.bank").value("NUBANK"))
                    .andExpect(jsonPath("$.currentBalance").value(1500.00))
                    .andExpect(jsonPath("$.deleted").value(false));

            verify(accountInternalService, times(1)).getAccountSummaryInternal(accountId);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a conta não for encontrada para o resumo interno")
        void shouldReturn404WhenAccountNotFoundForSummaryInternal() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            when(accountInternalService.getAccountSummaryInternal(notFoundId))
                    .thenThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            mockMvc.perform(get("/internal/accounts/internal-summary/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(accountInternalService, times(1)).getAccountSummaryInternal(notFoundId);
        }
    }

    @Nested
    @DisplayName("Cenários do GET /{accountId}/ownership")
    class CheckOwnershipTests {

        @Test
        @DisplayName("Deve retornar 200 OK com true quando o usuário for o dono da conta")
        void shouldReturn200AndTrueWhenUserIsAccountOwner() throws Exception {
            UUID accountId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(accountValidatorService.checkAccountOwner(eq(accountId), any(Jwt.class)))
                    .thenReturn(true);

            mockMvc.perform(get("/internal/accounts/{accountId}/ownership", accountId)
                            .with(jwt().jwt(builder -> builder.claim("sub", userId.toString())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(accountValidatorService, times(1)).checkAccountOwner(eq(accountId), any(Jwt.class));
        }

        @Test
        @DisplayName("Deve retornar 200 OK com false quando o usuário NÃO for o dono da conta")
        void shouldReturn200AndFalseWhenUserIsNotAccountOwner() throws Exception {
            UUID accountId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(accountValidatorService.checkAccountOwner(eq(accountId), any(Jwt.class)))
                    .thenReturn(false);

            mockMvc.perform(get("/internal/accounts/{accountId}/ownership", accountId)
                            .with(jwt().jwt(builder -> builder.claim("sub", userId.toString())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            verify(accountValidatorService, times(1)).checkAccountOwner(eq(accountId), any(Jwt.class));
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a conta informada não existir")
        void shouldReturn404WhenAccountNotFoundDuringOwnershipCheck() throws Exception {
            UUID notFoundAccountId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(accountValidatorService.checkAccountOwner(eq(notFoundAccountId), any(Jwt.class)))
                    .thenThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            mockMvc.perform(get("/internal/accounts/{accountId}/ownership", notFoundAccountId)
                            .with(jwt().jwt(builder -> builder.claim("sub", userId.toString())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(accountValidatorService, times(1)).checkAccountOwner(eq(notFoundAccountId), any(Jwt.class));
        }
    }
}