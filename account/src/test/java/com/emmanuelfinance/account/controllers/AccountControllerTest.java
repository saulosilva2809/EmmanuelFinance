package com.emmanuelfinance.account.controllers;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountTestDataBuilder;
import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.account.services.AccountService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@ActiveProfiles("test")
@WebMvcTest(AccountController.class)
@WithMockUser
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Nested
    @DisplayName("Cenários do POST /accounts")
    class CreateAccountTests {

        @Test
        @DisplayName("Deve retornar 400 Bad Request ao enviar DTO com dados inválidos")
        void shouldReturn400WhenCreateAccountWithInvalidData() throws Exception {
            CreateAccountDTO accountDTO = new CreateAccountDTO(null, null, null, null);

            mockMvc.perform(post("/accounts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountDTO)))
                    .andExpect(status().isBadRequest()
            );

            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("Deve retornar 201 Created quando tudo ocorrer com sucesso")
        void shouldReturn201WhenEverythingGoesWell() throws Exception {
            CreateAccountDTO accountDTO = new CreateAccountDTO(
                    "Conta Principal",
                    TypeEnum.CHECKING,
                    BanksEnum.NUBANK,
                    new BigDecimal(1000)
            );

            mockMvc.perform(post("/accounts")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(accountDTO)))
                    .andExpect(status().isCreated()
            );

            verify(accountService, times(1)).create(accountDTO);
        }
    }

    @Nested
    @DisplayName("Cenários do VIEW /accounts/{id}")
    class ViewAccountTests {

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a conta não for encontrada")
        void shouldReturn404WhenTheAccountIsNotFound() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            when(accountService.view(notFoundId)).thenThrow(
                    new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND)
            );

            mockMvc.perform(get("/accounts/{id}", notFoundId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(accountService, times(1)).view(notFoundId);
        }

        @Test
        @DisplayName("Deve retornar 200 Ok quando a conta for encontrada")
        void shouldReturn200WhenAccountIsFound() throws Exception {
            Account account = AccountTestDataBuilder.accountEntity(
                    AccountTestDataBuilder.createAccountDTO(),
                    UUID.randomUUID(),
                    false
            );

            when(accountService.view(account.getId())).thenReturn(
                    AccountTestDataBuilder.responseAccountDTO(account)
            );

            mockMvc.perform(get("/accounts/{id}", account.getUserId())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(accountService, times(1)).view(account.getUserId());
        }
    }

    @Nested
    @DisplayName("Cenários do GET /accounts (Listagem Paginada)")
    class ListAccountsTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os dados paginados quando a busca for bem-sucedida")
        void shouldReturn200WithPaginatedDataWhenListIsSuccessful() throws Exception {
            Account account = AccountTestDataBuilder.accountEntity(
                    AccountTestDataBuilder.createAccountDTO(),
                    UUID.randomUUID(),
                    false
            );
            ResponseAccountDTO responseAccountDTO = AccountTestDataBuilder.responseAccountDTO(account);

            PageResponseDTO<ResponseAccountDTO> pageResponse = new PageResponseDTO<>(
                    List.of(responseAccountDTO),
                    0,
                    10,
                    1L,
                    1
            );

            when(accountService.list(any(AccountFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/accounts")
                            .param("name", "Conta")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(responseAccountDTO.id().toString()))
                    .andExpect(jsonPath("$.content[0].name").value(responseAccountDTO.name()))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(accountService, times(1)).list(any(AccountFiltersDTO.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve retornar 200 OK com lista vazia no content quando não encontrar registros")
        void shouldReturn200WithEmptyContentWhenNoAccountsFound() throws Exception {
            PageResponseDTO<ResponseAccountDTO> emptyPageResponse = new PageResponseDTO<>(
                    Collections.emptyList(),
                    0,
                    10,
                    0L,
                    0
            );

            when(accountService.list(any(AccountFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPageResponse);

            mockMvc.perform(get("/accounts")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(accountService, times(1)).list(any(AccountFiltersDTO.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando os filtros de busca forem inválidos")
        void shouldReturn400WhenFiltersAreInvalid() throws Exception {
            mockMvc.perform(get("/accounts")
                            .param("type", "ENUM_INVALIDO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(accountService, atMostOnce()).list(any(AccountFiltersDTO.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Cenários do GET /accounts/deleted (Listagem Paginada de Contas Excluídas)")
    class ListDeletedAccountsTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os dados das contas excluídas quando a busca for bem-sucedida")
        void shouldReturn200WithPaginatedDataWhenListDeletedIsSuccessful() throws Exception {
            Account account = AccountTestDataBuilder.accountEntity(
                    AccountTestDataBuilder.createAccountDTO(),
                    UUID.randomUUID(),
                    true
            );
            ResponseAccountDTO responseAccountDTO = AccountTestDataBuilder.responseAccountDTO(account);

            PageResponseDTO<ResponseAccountDTO> pageResponse = new PageResponseDTO<>(
                    List.of(responseAccountDTO),
                    0,
                    10,
                    1L,
                    1
            );

            when(accountService.listDeleted(any(AccountFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/accounts/deleted")
                            .param("name", "Conta Deletada")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "createdAt,desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(responseAccountDTO.id().toString()))
                    .andExpect(jsonPath("$.content[0].name").value(responseAccountDTO.name()))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(accountService, times(1)).listDeleted(any(AccountFiltersDTO.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve retornar 200 OK com lista vazia no content quando não houver contas excluídas")
        void shouldReturn200WithEmptyContentWhenNoDeletedAccountsFound() throws Exception {
            PageResponseDTO<ResponseAccountDTO> emptyPageResponse = new PageResponseDTO<>(
                    Collections.emptyList(),
                    0,
                    10,
                    0L,
                    0
            );

            when(accountService.listDeleted(any(AccountFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPageResponse);

            mockMvc.perform(get("/accounts/deleted")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));

            verify(accountService, times(1)).listDeleted(any(AccountFiltersDTO.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando houver erro de filtro na listagem de excluídas")
        void shouldReturn400WhenFiltersAreInvalidInListDeleted() throws Exception {
            mockMvc.perform(get("/accounts/deleted")
                            .param("type", "ENUM_INVALIDO")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(accountService, atMostOnce()).listDeleted(any(AccountFiltersDTO.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Cenários do PUT /accounts/{id}")
    class UpdateAccountTests {

        @Test
        @DisplayName("Deve retornar 200 OK quando a atualização ocorrer com sucesso")
        void shouldReturn200WhenUpdateIsSuccessful() throws Exception {
            UUID accountId = UUID.randomUUID();
            UpdateAccountDTO updateDTO = new UpdateAccountDTO(
                    "Conta Atualizada",
                    TypeEnum.SAVINGS
            );

            Account account = AccountTestDataBuilder.accountEntity(
                    AccountTestDataBuilder.createAccountDTO(),
                    UUID.randomUUID(),
                    false
            );
            ResponseAccountDTO responseDTO = AccountTestDataBuilder.responseAccountDTO(account);

            when(accountService.update(eq(accountId), any(UpdateAccountDTO.class)))
                    .thenReturn(responseDTO);

            mockMvc.perform(put("/accounts/{id}", accountId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(responseDTO.id().toString()))
                    .andExpect(jsonPath("$.name").value(responseDTO.name()));

            verify(accountService, times(1)).update(eq(accountId), any(UpdateAccountDTO.class));
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a conta a ser atualizada não existir")
        void shouldReturn404WhenAccountToUpdateDoesNotExist() throws Exception {
            UUID notFoundId = UUID.randomUUID();
            UpdateAccountDTO updateDTO = new UpdateAccountDTO(
                    "Conta Atualizada",
                    TypeEnum.SAVINGS
            );

            when(accountService.update(eq(notFoundId), any(UpdateAccountDTO.class)))
                    .thenThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            mockMvc.perform(put("/accounts/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO)))
                    .andExpect(status().isNotFound());

            verify(accountService, times(1)).update(eq(notFoundId), any(UpdateAccountDTO.class));
        }
    }

    @Nested
    @DisplayName("Cenários do DELETE /accounts/{id}")
    class DeleteAccountTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando a exclusão for realizada com sucesso")
        void shouldReturn204WhenDeleteIsSuccessful() throws Exception {
            UUID accountId = UUID.randomUUID();

            doNothing().when(accountService).delete(accountId);

            mockMvc.perform(delete("/accounts/{id}", accountId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(accountService, times(1)).delete(accountId);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando tentar excluir uma conta inexistente")
        void shouldReturn404WhenAccountToDeleteDoesNotExist() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            doThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND))
                    .when(accountService).delete(notFoundId);

            mockMvc.perform(delete("/accounts/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(accountService, times(1)).delete(notFoundId);
        }
    }

    @Nested
    @DisplayName("Cenários do POST /accounts/restore/{id}")
    class RestoreAccountTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando a restauração for realizada com sucesso")
        void shouldReturn204WhenRestoreIsSuccessful() throws Exception {
            UUID accountId = UUID.randomUUID();

            doNothing().when(accountService).restore(accountId);

            mockMvc.perform(post("/accounts/restore/{id}", accountId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(accountService, times(1)).restore(accountId);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando tentar restaurar uma conta inexistente")
        void shouldReturn404WhenAccountToRestoreDoesNotExist() throws Exception {
            UUID notFoundId = UUID.randomUUID();

            doThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND))
                    .when(accountService).restore(notFoundId);

            mockMvc.perform(post("/accounts/restore/{id}", notFoundId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(accountService, times(1)).restore(notFoundId);
        }
    }
}
