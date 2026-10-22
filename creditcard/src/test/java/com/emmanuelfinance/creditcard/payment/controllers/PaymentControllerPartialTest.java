package com.emmanuelfinance.creditcard.payment.controllers;

import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.payment.dto.PaymentPartialDTO;
import com.emmanuelfinance.creditcard.payment.exceptions.PaymentDomainException;
import com.emmanuelfinance.creditcard.payment.exceptions.PaymentErrorCode;
import com.emmanuelfinance.creditcard.payment.services.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(PaymentController.class)
@WithMockUser
public class PaymentControllerPartialTest {

    private static final String URL = "/credit-card/{creditCardId}/payment/{invoiceId}/partial";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private PaymentService paymentService;

    private final UUID cardId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    private ResultActions pay(Object body) throws Exception {
        return mockMvc.perform(post(URL, cardId, invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    @DisplayName("Deve retornar 204 No Content e repassar os ids e o valor ao service")
    void shouldReturn204WhenPaymentIsSuccessful() throws Exception {
        PaymentPartialDTO body = new PaymentPartialDTO(accountId, new BigDecimal("100.00"));

        pay(body).andExpect(status().isNoContent());

        verify(paymentService, times(1)).partialInvoicePayment(cardId, invoiceId, body);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o accountId não for informado")
    void shouldReturn400WhenAccountIdIsMissing() throws Exception {
        pay(new PaymentPartialDTO(null, new BigDecimal("100.00"))).andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o valor não for informado")
    void shouldReturn400WhenAmountIsMissing() throws Exception {
        pay(new PaymentPartialDTO(accountId, null)).andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o valor passar do total da fatura")
    void shouldReturn400WhenAmountIsGreaterThanTheTotal() throws Exception {
        doThrow(new PaymentDomainException(PaymentErrorCode.DATA_INVALID_VALUE))
                .when(paymentService).partialInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentPartialDTO.class));

        pay(new PaymentPartialDTO(accountId, new BigDecimal("999.00"))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a fatura não existir")
    void shouldReturn404WhenInvoiceIsNotFound() throws Exception {
        doThrow(new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND))
                .when(paymentService).partialInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentPartialDTO.class));

        pay(new PaymentPartialDTO(accountId, new BigDecimal("100.00"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 422 quando o saldo da conta for insuficiente")
    void shouldReturn422WhenBalanceIsInsufficient() throws Exception {
        doThrow(new PaymentDomainException(PaymentErrorCode.INSUFFICIENT_BALANCE))
                .when(paymentService).partialInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentPartialDTO.class));

        pay(new PaymentPartialDTO(accountId, new BigDecimal("100.00"))).andExpect(status().isUnprocessableEntity());
    }
}
