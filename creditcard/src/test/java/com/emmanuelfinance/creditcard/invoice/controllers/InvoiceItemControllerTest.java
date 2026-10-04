package com.emmanuelfinance.creditcard.invoice.controllers;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceItemDTO;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceItemService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(InvoiceItemController.class)
@WithMockUser
public class InvoiceItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private InvoiceItemService invoiceItemService;

    @Test
    @DisplayName("Deve retornar 200 OK com os itens paginados da fatura")
    void shouldReturn200WithPaginatedItems() throws Exception {
        UUID userId = UUID.randomUUID();
        Invoice invoice = InvoiceTestDataBuilder.invoiceEntity(userId, UUID.randomUUID());
        InvoiceItem item = InvoiceTestDataBuilder.invoiceItemEntity(userId, invoice.getId(), UUID.randomUUID());
        ResponseInvoiceItemDTO dto = InvoiceTestDataBuilder.responseInvoiceItemDTO(
                item,
                InvoiceTestDataBuilder.responseInvoiceSummaryDTO(invoice),
                InvoiceTestDataBuilder.transactionSummaryDTO()
        );
        when(invoiceItemService.listByInvoiceId(eq(invoice.getId()), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(List.of(dto), 0, 10, 1L, 1));

        mockMvc.perform(get("/credit-card/invoice-item/{invoiceId}", invoice.getId())
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoice.id").value(invoice.getId().toString()))
                .andExpect(jsonPath("$.content[0].installmentNumber").value(1))
                .andExpect(jsonPath("$.content[0].totalInstallments").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(invoiceItemService, times(1)).listByInvoiceId(eq(invoice.getId()), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve retornar 200 OK com content vazio quando a fatura não tiver itens")
    void shouldReturn200WithEmptyContent() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceItemService.listByInvoiceId(eq(invoiceId), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(Collections.emptyList(), 0, 10, 0L, 0));

        mockMvc.perform(get("/credit-card/invoice-item/{invoiceId}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Deve aplicar a ordenação padrão installmentNumber,createdAt ASC")
    void shouldApplyDefaultSort() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceItemService.listByInvoiceId(eq(invoiceId), any(Pageable.class)))
                .thenReturn(new PageResponseDTO<>(Collections.emptyList(), 0, 10, 0L, 0));

        mockMvc.perform(get("/credit-card/invoice-item/{invoiceId}", invoiceId)).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(invoiceItemService).listByInvoiceId(eq(invoiceId), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("installmentNumber").getDirection());
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a fatura não existir")
    void shouldReturn404WhenInvoiceDoesNotExist() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceItemService.listByInvoiceId(eq(invoiceId), any(Pageable.class)))
                .thenThrow(new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND));

        mockMvc.perform(get("/credit-card/invoice-item/{invoiceId}", invoiceId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o invoiceId não for UUID")
    void shouldReturn400WhenInvoiceIdIsInvalid() throws Exception {
        mockMvc.perform(get("/credit-card/invoice-item/{invoiceId}", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(invoiceItemService);
    }
}
