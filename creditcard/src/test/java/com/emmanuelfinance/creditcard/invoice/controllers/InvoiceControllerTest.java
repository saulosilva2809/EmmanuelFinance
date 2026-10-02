package com.emmanuelfinance.creditcard.invoice.controllers;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import org.junit.jupiter.api.DisplayName;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(InvoiceController.class)
@WithMockUser
public class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    @DisplayName("Deve retornar 200 OK com os dados paginados")
    void shouldReturn200WithPaginatedData() throws Exception {
        UUID creditCardId = UUID.randomUUID();
        Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), creditCardId);
        ResponseInvoiceDTO dto = InvoiceTestDataBuilder.responseInvoiceDTO(
                invoice, InvoiceTestDataBuilder.creditCardSummaryDTO(creditCardId)
        );
        when(invoiceService.list(any(InvoiceFiltersDTO.class), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(List.of(dto), 0, 10, 1L, 1));

        mockMvc.perform(get("/credit-card/invoices")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(dto.id().toString()))
                .andExpect(jsonPath("$.content[0].month").value(10))
                .andExpect(jsonPath("$.content[0].year").value(2026))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(invoiceService, times(1)).list(any(InvoiceFiltersDTO.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve retornar 200 OK com content vazio quando não houver faturas")
    void shouldReturn200WithEmptyContent() throws Exception {
        when(invoiceService.list(any(InvoiceFiltersDTO.class), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(Collections.emptyList(), 0, 10, 0L, 0));

        mockMvc.perform(get("/credit-card/invoices")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Deve repassar os filtros da query string ao service")
    void shouldPassFiltersToService() throws Exception {
        UUID creditCardId = UUID.randomUUID();
        when(invoiceService.list(any(InvoiceFiltersDTO.class), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(Collections.emptyList(), 0, 10, 0L, 0));

        mockMvc.perform(get("/credit-card/invoices")
                        .param("creditCardId", creditCardId.toString())
                        .param("month", "10")
                        .param("year", "2026")
                        .param("status", "PAID"))
                .andExpect(status().isOk());

        ArgumentCaptor<InvoiceFiltersDTO> captor = ArgumentCaptor.forClass(InvoiceFiltersDTO.class);
        verify(invoiceService).list(captor.capture(), any(Pageable.class));
        assertEquals(new InvoiceFiltersDTO(creditCardId, 10, 2026, InvoiceStatusEnum.PAID), captor.getValue());
    }

    @Test
    @DisplayName("Deve aplicar a ordenação padrão year,month ASC")
    void shouldApplyDefaultSort() throws Exception {
        when(invoiceService.list(any(InvoiceFiltersDTO.class), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(Collections.emptyList(), 0, 10, 0L, 0));

        mockMvc.perform(get("/credit-card/invoices")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(invoiceService).list(any(InvoiceFiltersDTO.class), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("year").getDirection());
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("month").getDirection());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o status for inválido")
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/credit-card/invoices").param("status", "INVALIDO"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(invoiceService);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o creditCardId não for UUID")
    void shouldReturn400WhenCreditCardIdIsInvalid() throws Exception {
        mockMvc.perform(get("/credit-card/invoices").param("creditCardId", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(invoiceService);
    }
}
