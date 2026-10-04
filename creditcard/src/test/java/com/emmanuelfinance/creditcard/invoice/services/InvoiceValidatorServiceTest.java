package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceValidatorServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceValidatorService invoiceValidatorService;

    @Test
    @DisplayName("Não deve lançar erro quando a fatura existir e não estiver excluída")
    void shouldPassWhenInvoiceExists() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.existsByIdAndDeletedFalse(invoiceId)).thenReturn(true);

        assertDoesNotThrow(() -> invoiceValidatorService.existsById(invoiceId));

        verify(invoiceRepository, times(1)).existsByIdAndDeletedFalse(invoiceId);
    }

    @Test
    @DisplayName("Deve lançar INVOICE_NOT_FOUND (404) quando a fatura não existir ou estiver excluída")
    void shouldThrowWhenInvoiceDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.existsByIdAndDeletedFalse(invoiceId)).thenReturn(false);

        InvoiceDomainException exception = assertThrows(
                InvoiceDomainException.class,
                () -> invoiceValidatorService.existsById(invoiceId)
        );

        assertEquals(InvoiceErrorCode.INVOICE_NOT_FOUND, exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
