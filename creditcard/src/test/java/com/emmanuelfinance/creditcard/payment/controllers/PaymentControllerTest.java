package com.emmanuelfinance.creditcard.payment.controllers;

import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.payment.dto.PaymentPartialDTO;
import com.emmanuelfinance.creditcard.payment.dto.PaymentTotalDTO;
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
public class PaymentControllerTest {

    private static final String URL = "/credit-card/{creditCardId}/payment/{invoiceId}/total";
    private static final String PARTIAL_URL = "/credit-card/{creditCardId}/payment/{invoiceId}/partial";

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

    private org.springframework.test.web.servlet.ResultActions pay(Object body) throws Exception {
        return mockMvc.perform(post(URL, cardId, invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    @DisplayName("Deve retornar 204 No Content e repassar os ids ao service")
    void shouldReturn204WhenPaymentIsSuccessful() throws Exception {
        pay(new PaymentTotalDTO(accountId)).andExpect(status().isNoContent());

        verify(paymentService, times(1)).totalInvoicePayment(cardId, invoiceId, new PaymentTotalDTO(accountId));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o accountId não for informado")
    void shouldReturn400WhenAccountIdIsMissing() throws Exception {
        pay(new PaymentTotalDTO(null)).andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando um id do path não for UUID")
    void shouldReturn400WhenPathIdIsInvalid() throws Exception {
        mockMvc.perform(post(URL, "abc", invoiceId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentTotalDTO(accountId))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a fatura não existir")
    void shouldReturn404WhenInvoiceIsNotFound() throws Exception {
        doThrow(new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND))
                .when(paymentService).totalInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentTotalDTO.class));

        pay(new PaymentTotalDTO(accountId)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a fatura já estiver paga")
    void shouldReturn409WhenInvoiceIsAlreadyPaid() throws Exception {
        doThrow(new PaymentDomainException(PaymentErrorCode.INVOICE_ALREADY_PAID))
                .when(paymentService).totalInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentTotalDTO.class));

        pay(new PaymentTotalDTO(accountId)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve retornar 422 quando o saldo da conta for insuficiente")
    void shouldReturn422WhenBalanceIsInsufficient() throws Exception {
        doThrow(new PaymentDomainException(PaymentErrorCode.INSUFFICIENT_BALANCE))
                .when(paymentService).totalInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentTotalDTO.class));

        pay(new PaymentTotalDTO(accountId)).andExpect(status().isUnprocessableEntity());
    }

    private org.springframework.test.web.servlet.ResultActions payPartial(Object body) throws Exception {
        return mockMvc.perform(post(PARTIAL_URL, cardId, invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    @Test
    @DisplayName("Parcial: deve retornar 204 No Content e repassar os ids e o valor ao service")
    void shouldReturn204WhenPartialPaymentIsSuccessful() throws Exception {
        PaymentPartialDTO body = new PaymentPartialDTO(accountId, new BigDecimal("100.00"));

        payPartial(body).andExpect(status().isNoContent());

        verify(paymentService, times(1)).partialInvoicePayment(cardId, invoiceId, body);
    }

    @Test
    @DisplayName("Parcial: deve retornar 400 Bad Request quando o accountId não for informado")
    void shouldReturn400WhenPartialAccountIdIsMissing() throws Exception {
        payPartial(new PaymentPartialDTO(null, new BigDecimal("100.00"))).andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Parcial: deve retornar 400 Bad Request quando o valor não for informado")
    void shouldReturn400WhenAmountIsMissing() throws Exception {
        payPartial(new PaymentPartialDTO(accountId, null)).andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Parcial: deve retornar 400 Bad Request quando o valor for zero")
    void shouldReturn400WhenAmountIsZero() throws Exception {
        payPartial(new PaymentPartialDTO(accountId, BigDecimal.ZERO)).andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Parcial: deve retornar 400 Bad Request quando o valor for negativo")
    void shouldReturn400WhenAmountIsNegative() throws Exception {
        payPartial(new PaymentPartialDTO(accountId, new BigDecimal("-10.00"))).andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("Parcial: deve retornar 400 Bad Request quando o valor passar do que falta pagar")
    void shouldReturn400WhenAmountIsGreaterThanTheRemaining() throws Exception {
        doThrow(new PaymentDomainException(PaymentErrorCode.DATA_INVALID_VALUE))
                .when(paymentService).partialInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentPartialDTO.class));

        payPartial(new PaymentPartialDTO(accountId, new BigDecimal("999.00"))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Parcial: deve retornar 404 Not Found quando a fatura não existir")
    void shouldReturn404WhenPartialInvoiceIsNotFound() throws Exception {
        doThrow(new InvoiceDomainException(InvoiceErrorCode.INVOICE_NOT_FOUND))
                .when(paymentService).partialInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentPartialDTO.class));

        payPartial(new PaymentPartialDTO(accountId, new BigDecimal("100.00"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Parcial: deve retornar 422 quando o saldo da conta for insuficiente")
    void shouldReturn422WhenPartialBalanceIsInsufficient() throws Exception {
        doThrow(new PaymentDomainException(PaymentErrorCode.INSUFFICIENT_BALANCE))
                .when(paymentService).partialInvoicePayment(eq(cardId), eq(invoiceId), any(PaymentPartialDTO.class));

        payPartial(new PaymentPartialDTO(accountId, new BigDecimal("100.00"))).andExpect(status().isUnprocessableEntity());
    }
}
