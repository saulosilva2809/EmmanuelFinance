package com.emmanuelfinance.transaction.controllers;

import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.dtos.TransactionFiltersDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;
import com.emmanuelfinance.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.exceptions.TransactionErrorCode;
import com.emmanuelfinance.transaction.services.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(TransactionController.class)
@WithMockUser
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private TransactionService transactionService;

    private ResponseTransactionDTO responseDTO(boolean deleted) {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID(), deleted);
        return TransactionTestDataBuilder.responseDTO(transaction);
    }

    private PageResponseDTO<ResponseTransactionDTO> pageOf(ResponseTransactionDTO dto) {
        return new PageResponseDTO<>(List.of(dto), 0, 10, 1L, 1);
    }

    private PageResponseDTO<ResponseTransactionDTO> emptyPage() {
        return new PageResponseDTO<>(Collections.emptyList(), 0, 10, 0L, 0);
    }

    @Nested
    @DisplayName("Cenários do POST /transactions")
    class CreateTests {

        @Test
        @DisplayName("Deve retornar 201 Created quando tudo ocorrer com sucesso")
        void shouldReturn201WhenEverythingGoesWell() throws Exception {
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO();
            ResponseTransactionDTO response = responseDTO(false);
            when(transactionService.create(any(CreateTransactionDTO.class))).thenReturn(response);

            mockMvc.perform(post("/transactions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(response.id().toString()))
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.type").value("EXPENSE"));

            ArgumentCaptor<CreateTransactionDTO> captor = ArgumentCaptor.forClass(CreateTransactionDTO.class);
            verify(transactionService, times(1)).create(captor.capture());
            assertEquals(dto, captor.getValue());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request ao enviar DTO com campos obrigatórios nulos")
        void shouldReturn400WhenRequiredFieldsAreNull() throws Exception {
            CreateTransactionDTO dto = TransactionTestDataBuilder.createDTO(null, null, null, null, null, false, null, null);

            mockMvc.perform(post("/transactions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o tipo for inválido")
        void shouldReturn400WhenTypeIsInvalid() throws Exception {
            String body = "{\"accountId\":\"" + UUID.randomUUID() + "\",\"categoryId\":\"" + UUID.randomUUID()
                    + "\",\"amount\":100,\"type\":\"TIPO_INVALIDO\"}";

            mockMvc.perform(post("/transactions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando a transação já tiver sido feita")
        void shouldReturn409WhenTransactionAlreadyExists() throws Exception {
            when(transactionService.create(any(CreateTransactionDTO.class))).thenThrow(
                    new TransactionDomainException(TransactionErrorCode.TRANSACTION_ALREADY_EXISTS)
            );

            mockMvc.perform(post("/transactions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionTestDataBuilder.createDTO())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Deve retornar 422 quando houver parcelas em transação de conta")
        void shouldReturn422WhenInstallmentsInAccountTransaction() throws Exception {
            when(transactionService.create(any(CreateTransactionDTO.class))).thenThrow(
                    new TransactionDomainException(TransactionErrorCode.INSTALLMENTS_IN_TRANSACTION_ACCOUNT)
            );

            mockMvc.perform(post("/transactions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionTestDataBuilder.createDTO())))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("Cenários do GET /transactions/{id}")
    class ViewTests {

        @Test
        @DisplayName("Deve retornar 200 OK quando a transação for encontrada")
        void shouldReturn200WhenFound() throws Exception {
            ResponseTransactionDTO response = responseDTO(false);
            when(transactionService.view(response.id())).thenReturn(response);

            mockMvc.perform(get("/transactions/{id}", response.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(response.id().toString()))
                    .andExpect(jsonPath("$.amount").value(100.00));

            verify(transactionService, times(1)).view(response.id());
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a transação não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(transactionService.view(id)).thenThrow(new TransactionNotFound());

            mockMvc.perform(get("/transactions/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o id não for UUID")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(get("/transactions/{id}", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }
    }

    @Nested
    @DisplayName("Cenários do GET /transactions")
    class ListTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os dados paginados")
        void shouldReturn200WithPaginatedData() throws Exception {
            ResponseTransactionDTO response = responseDTO(false);
            when(transactionService.list(any(TransactionFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(pageOf(response));

            mockMvc.perform(get("/transactions").param("page", "0").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(response.id().toString()))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("Deve retornar 200 OK com content vazio")
        void shouldReturn200WithEmptyContent() throws Exception {
            when(transactionService.list(any(TransactionFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Deve repassar os filtros da query string ao service")
        void shouldPassFiltersToService() throws Exception {
            UUID accountId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            UUID cardId = UUID.randomUUID();
            when(transactionService.list(any(TransactionFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/transactions")
                            .param("accountId", accountId.toString())
                            .param("categoryId", categoryId.toString())
                            .param("creditCardId", cardId.toString())
                            .param("greaterValueThan", "10.50")
                            .param("valueLessThan", "99.90")
                            .param("scheduled", "true")
                            .param("date", "2026-10-05")
                            .param("status", "PENDING")
                            .param("type", "EXPENSE"))
                    .andExpect(status().isOk());

            ArgumentCaptor<TransactionFiltersDTO> captor = ArgumentCaptor.forClass(TransactionFiltersDTO.class);
            verify(transactionService).list(captor.capture(), any(Pageable.class));
            assertEquals(new TransactionFiltersDTO(
                    accountId, categoryId, cardId,
                    new BigDecimal("10.50"), new BigDecimal("99.90"),
                    true, LocalDate.of(2026, 10, 5),
                    StatusTransactionEnum.PENDING, TypeEnum.EXPENSE
            ), captor.getValue());
        }

        @Test
        @DisplayName("Deve aplicar a ordenação padrão createdAt DESC")
        void shouldApplyDefaultSort() throws Exception {
            when(transactionService.list(any(TransactionFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/transactions")).andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(transactionService).list(any(TransactionFiltersDTO.class), captor.capture());
            assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("createdAt").getDirection());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando status ou tipo do filtro forem inválidos")
        void shouldReturn400WhenFiltersAreInvalid() throws Exception {
            mockMvc.perform(get("/transactions").param("status", "INVALIDO"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(get("/transactions").param("type", "INVALIDO"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }
    }

    @Nested
    @DisplayName("Cenários do GET /transactions/deleted")
    class ListDeletedTests {

        @Test
        @DisplayName("Deve retornar 200 OK com as transações excluídas paginadas")
        void shouldReturn200WithPaginatedData() throws Exception {
            ResponseTransactionDTO response = responseDTO(true);
            when(transactionService.listDeleted(any(TransactionFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(pageOf(response));

            mockMvc.perform(get("/transactions/deleted"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(response.id().toString()))
                    .andExpect(jsonPath("$.content[0].deleted").value(true))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Deve retornar 200 OK com content vazio")
        void shouldReturn200WithEmptyContent() throws Exception {
            when(transactionService.listDeleted(any(TransactionFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/transactions/deleted"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Deve aplicar a ordenação padrão createdAt DESC")
        void shouldApplyDefaultSort() throws Exception {
            when(transactionService.listDeleted(any(TransactionFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/transactions/deleted")).andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(transactionService).listDeleted(any(TransactionFiltersDTO.class), captor.capture());
            assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("createdAt").getDirection());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o filtro for inválido")
        void shouldReturn400WhenFilterIsInvalid() throws Exception {
            mockMvc.perform(get("/transactions/deleted").param("type", "INVALIDO"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }
    }

    @Nested
    @DisplayName("Cenários do PUT /transactions/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Deve retornar 200 OK quando a atualização ocorrer com sucesso")
        void shouldReturn200WhenUpdateIsSuccessful() throws Exception {
            ResponseTransactionDTO response = responseDTO(false);
            when(transactionService.update(eq(response.id()), any(UpdateTransactionDTO.class))).thenReturn(response);

            mockMvc.perform(put("/transactions/{id}", response.id())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionTestDataBuilder.updateDTO())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(response.id().toString()));

            verify(transactionService, times(1)).update(eq(response.id()), any(UpdateTransactionDTO.class));
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a transação não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(transactionService.update(eq(id), any(UpdateTransactionDTO.class))).thenThrow(new TransactionNotFound());

            mockMvc.perform(put("/transactions/{id}", id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionTestDataBuilder.updateDTO())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 quando tentar alterar transação feita com cartão")
        void shouldReturn422WhenTransactionHasCard() throws Exception {
            UUID id = UUID.randomUUID();
            when(transactionService.update(eq(id), any(UpdateTransactionDTO.class))).thenThrow(
                    new TransactionDomainException(TransactionErrorCode.CARD_TRANSACTIONS_CANNOT_BE_CHANGED)
            );

            mockMvc.perform(put("/transactions/{id}", id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionTestDataBuilder.updateDTO())))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o id não for UUID")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(put("/transactions/{id}", "abc")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(TransactionTestDataBuilder.updateDTO())))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(transactionService);
        }
    }

    @Nested
    @DisplayName("Cenários do DELETE /transactions/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando a exclusão ocorrer com sucesso")
        void shouldReturn204WhenDeleteIsSuccessful() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/transactions/{id}", id).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(transactionService, times(1)).delete(id);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a transação não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new TransactionNotFound()).when(transactionService).delete(id);

            mockMvc.perform(delete("/transactions/{id}", id).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Cenários do POST /transactions/restore/{id}")
    class RestoreTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando a restauração ocorrer com sucesso")
        void shouldReturn204WhenRestoreIsSuccessful() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(post("/transactions/restore/{id}", id).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(transactionService, times(1)).restore(id);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando a transação não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new TransactionNotFound()).when(transactionService).restore(id);

            mockMvc.perform(post("/transactions/restore/{id}", id).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando a transação não estiver excluída")
        void shouldReturn409WhenTransactionIsNotDeleted() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new TransactionDomainException(TransactionErrorCode.RESTORE_TRANSACTION_NOT_DELETED))
                    .when(transactionService).restore(id);

            mockMvc.perform(post("/transactions/restore/{id}", id).with(csrf()))
                    .andExpect(status().isConflict());
        }
    }
}
