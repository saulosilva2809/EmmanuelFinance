package com.emmanuelfinance.creditcard.controllers;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.CreditCardTestDataBuilder;
import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.exceptions.CreditCardDomainException;
import com.emmanuelfinance.creditcard.exceptions.CreditCardErrorCode;
import com.emmanuelfinance.creditcard.services.CreditCardService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
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
@WebMvcTest(CreditCardController.class)
@WithMockUser
public class CreditCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private CreditCardService creditCardService;

    private ResponseCreditCardDTO responseDTO(boolean deleted) {
        CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
        CreditCard card = CreditCardTestDataBuilder.createEntity(dto, UUID.randomUUID(), deleted);
        return CreditCardTestDataBuilder.responseCategoryDTO(
                card, CreditCardTestDataBuilder.accountSummaryInternalDTO(dto.accountId())
        );
    }

    private PageResponseDTO<ResponseCreditCardDTO> pageOf(ResponseCreditCardDTO dto) {
        return new PageResponseDTO<>(List.of(dto), 0, 10, 1L, 1);
    }

    private PageResponseDTO<ResponseCreditCardDTO> emptyPage() {
        return new PageResponseDTO<>(Collections.emptyList(), 0, 10, 0L, 0);
    }

    @Nested
    @DisplayName("Cenários do POST /credit-card")
    class CreateTests {

        @Test
        @DisplayName("Deve retornar 201 Created quando tudo ocorrer com sucesso")
        void shouldReturn201WhenEverythingGoesWell() throws Exception {
            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            ResponseCreditCardDTO response = responseDTO(false);
            when(creditCardService.create(dto)).thenReturn(response);

            mockMvc.perform(post("/credit-card")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(response.id().toString()))
                    .andExpect(jsonPath("$.name").value(response.name()))
                    .andExpect(jsonPath("$.bank").value(response.bank().name()));

            verify(creditCardService, times(1)).create(dto);
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request ao enviar DTO com dados inválidos")
        void shouldReturn400WhenDataIsInvalid() throws Exception {
            CreateCreditCardDTO dto = new CreateCreditCardDTO(null, null, null, null, null, null);

            mockMvc.perform(post("/credit-card")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(creditCardService);
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o banco for inválido")
        void shouldReturn400WhenBankIsInvalid() throws Exception {
            String body = "{\"accountId\":\"" + UUID.randomUUID() + "\",\"name\":\"Cartao\","
                    + "\"bank\":\"BANCO_INVALIDO\",\"creditLimit\":1000,\"closingDay\":17,\"dueDay\":24}";

            mockMvc.perform(post("/credit-card")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(creditCardService);
        }

        @Test
        @DisplayName("Deve retornar 422 quando o banco do cartão e da conta forem diferentes")
        void shouldReturn422WhenBanksAreDifferent() throws Exception {
            CreateCreditCardDTO dto = CreditCardTestDataBuilder.createCardDTO();
            when(creditCardService.create(dto)).thenThrow(
                    new CreditCardDomainException(CreditCardErrorCode.BANK_OF_CARD_AND_ACCOUNT_DIFFERENT)
            );

            mockMvc.perform(post("/credit-card")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("Cenários do GET /credit-card/{id}")
    class ViewTests {

        @Test
        @DisplayName("Deve retornar 200 OK quando o cartão for encontrado")
        void shouldReturn200WhenFound() throws Exception {
            ResponseCreditCardDTO response = responseDTO(false);
            when(creditCardService.view(response.id())).thenReturn(response);

            mockMvc.perform(get("/credit-card/{id}", response.id()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(response.id().toString()))
                    .andExpect(jsonPath("$.name").value(response.name()));

            verify(creditCardService, times(1)).view(response.id());
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando o cartão não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(creditCardService.view(id)).thenThrow(new CreditCardNotFound());

            mockMvc.perform(get("/credit-card/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o id não for UUID")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(get("/credit-card/{id}", "abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(creditCardService);
        }
    }

    @Nested
    @DisplayName("Cenários do GET /credit-card")
    class ListTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os dados paginados")
        void shouldReturn200WithPaginatedData() throws Exception {
            ResponseCreditCardDTO response = responseDTO(false);
            when(creditCardService.list(any(CreditCardFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(pageOf(response));

            mockMvc.perform(get("/credit-card")
                            .param("page", "0")
                            .param("size", "10"))
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
            when(creditCardService.list(any(CreditCardFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/credit-card"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Deve repassar os filtros ao service")
        void shouldPassFiltersToService() throws Exception {
            UUID accountId = UUID.randomUUID();
            when(creditCardService.list(any(CreditCardFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/credit-card")
                            .param("accountId", accountId.toString())
                            .param("name", "Cartao")
                            .param("bank", "C6_BANK"))
                    .andExpect(status().isOk());

            ArgumentCaptor<CreditCardFiltersDTO> captor = ArgumentCaptor.forClass(CreditCardFiltersDTO.class);
            verify(creditCardService).list(captor.capture(), any(Pageable.class));
            assertEquals(new CreditCardFiltersDTO(accountId, "Cartao", BanksEnum.C6_BANK), captor.getValue());
        }

        @Test
        @DisplayName("Deve aplicar a ordenação padrão createdAt DESC")
        void shouldApplyDefaultSort() throws Exception {
            when(creditCardService.list(any(CreditCardFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/credit-card")).andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(creditCardService).list(any(CreditCardFiltersDTO.class), captor.capture());
            assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("createdAt").getDirection());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o banco do filtro for inválido")
        void shouldReturn400WhenFilterIsInvalid() throws Exception {
            mockMvc.perform(get("/credit-card").param("bank", "INVALIDO"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(creditCardService);
        }
    }

    @Nested
    @DisplayName("Cenários do GET /credit-card/deleted")
    class ListDeletedTests {

        @Test
        @DisplayName("Deve retornar 200 OK com os cartões excluídos paginados")
        void shouldReturn200WithPaginatedData() throws Exception {
            ResponseCreditCardDTO response = responseDTO(true);
            when(creditCardService.listDeleted(any(CreditCardFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(pageOf(response));

            mockMvc.perform(get("/credit-card/deleted"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(response.id().toString()))
                    .andExpect(jsonPath("$.content[0].deleted").value(true))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Deve retornar 200 OK com content vazio")
        void shouldReturn200WithEmptyContent() throws Exception {
            when(creditCardService.listDeleted(any(CreditCardFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/credit-card/deleted"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Deve aplicar a ordenação padrão createdAt DESC")
        void shouldApplyDefaultSort() throws Exception {
            when(creditCardService.listDeleted(any(CreditCardFiltersDTO.class), any(Pageable.class)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/credit-card/deleted")).andExpect(status().isOk());

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(creditCardService).listDeleted(any(CreditCardFiltersDTO.class), captor.capture());
            assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("createdAt").getDirection());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o banco do filtro for inválido")
        void shouldReturn400WhenFilterIsInvalid() throws Exception {
            mockMvc.perform(get("/credit-card/deleted").param("bank", "INVALIDO"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(creditCardService);
        }
    }

    @Nested
    @DisplayName("Cenários do PUT /credit-card/{id}")
    class UpdateTests {

        private UpdateCreditCardDTO updateDTO() {
            return new UpdateCreditCardDTO(
                    UUID.randomUUID(), "Cartao Atualizado", BanksEnum.C6_BANK, new BigDecimal("5000"), 10, 20
            );
        }

        @Test
        @DisplayName("Deve retornar 200 OK quando a atualização ocorrer com sucesso")
        void shouldReturn200WhenUpdateIsSuccessful() throws Exception {
            ResponseCreditCardDTO response = responseDTO(false);
            when(creditCardService.update(eq(response.id()), any(UpdateCreditCardDTO.class))).thenReturn(response);

            mockMvc.perform(put("/credit-card/{id}", response.id())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(response.id().toString()))
                    .andExpect(jsonPath("$.name").value(response.name()));

            verify(creditCardService, times(1)).update(eq(response.id()), any(UpdateCreditCardDTO.class));
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando o cartão não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(creditCardService.update(eq(id), any(UpdateCreditCardDTO.class))).thenThrow(new CreditCardNotFound());

            mockMvc.perform(put("/credit-card/{id}", id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 422 quando o novo limite for negativo")
        void shouldReturn422WhenLimitIsNegative() throws Exception {
            UUID id = UUID.randomUUID();
            when(creditCardService.update(eq(id), any(UpdateCreditCardDTO.class))).thenThrow(
                    new CreditCardDomainException(CreditCardErrorCode.THE_CARD_LIMIT_CANNOT_BE_NEGATIVE)
            );

            mockMvc.perform(put("/credit-card/{id}", id)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO())))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Deve retornar 400 Bad Request quando o id não for UUID")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(put("/credit-card/{id}", "abc")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDTO())))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(creditCardService);
        }
    }

    @Nested
    @DisplayName("Cenários do DELETE /credit-card/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando a exclusão ocorrer com sucesso")
        void shouldReturn204WhenDeleteIsSuccessful() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/credit-card/{id}", id).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(creditCardService, times(1)).delete(id);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando o cartão não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new CreditCardNotFound()).when(creditCardService).delete(id);

            mockMvc.perform(delete("/credit-card/{id}", id).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Cenários do POST /credit-card/restore/{id}")
    class RestoreTests {

        @Test
        @DisplayName("Deve retornar 204 No Content quando a restauração ocorrer com sucesso")
        void shouldReturn204WhenRestoreIsSuccessful() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(post("/credit-card/restore/{id}", id).with(csrf()))
                    .andExpect(status().isNoContent());

            verify(creditCardService, times(1)).restore(id);
        }

        @Test
        @DisplayName("Deve retornar 404 Not Found quando o cartão não existir")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new CreditCardNotFound()).when(creditCardService).restore(id);

            mockMvc.perform(post("/credit-card/restore/{id}", id).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 409 Conflict quando o cartão não estiver excluído")
        void shouldReturn409WhenCardIsNotDeleted() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new CreditCardDomainException(CreditCardErrorCode.RESTORE_CARD_NOT_DELETED))
                    .when(creditCardService).restore(id);

            mockMvc.perform(post("/credit-card/restore/{id}", id).with(csrf()))
                    .andExpect(status().isConflict());
        }
    }
}
