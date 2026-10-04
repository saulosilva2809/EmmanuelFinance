package com.emmanuelfinance.creditcard.invoice.services;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceAmountServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceAmountService invoiceAmountService;

    private final UUID userId = UUID.randomUUID();

    private Invoice invoice(String total, boolean deleted) {
        return InvoiceTestDataBuilder.invoiceEntity(
                userId, UUID.randomUUID(), 10, 2026, new BigDecimal(total), InvoiceStatusEnum.OPEN, deleted
        );
    }

    @Nested
    @DisplayName("Cenários do removeBalanceAfterDeletion")
    class RemoveBalanceTests {

        @Test
        @DisplayName("Deve subtrair o valor de cada fatura e salvar todas")
        void shouldSubtractAmountFromEveryInvoice() {
            Invoice invoice1 = invoice("100.00", false);
            Invoice invoice2 = invoice("80.00", false);
            List<Invoice> invoices = List.of(invoice1, invoice2);

            invoiceAmountService.removeBalanceAfterDeletion(invoices, new BigDecimal("30.00"));

            assertEquals(new BigDecimal("70.00"), invoice1.getTotalAmount());
            assertEquals(new BigDecimal("50.00"), invoice2.getTotalAmount());
            assertFalse(invoice1.isDeleted());
            assertFalse(invoice2.isDeleted());
            verify(invoiceRepository, times(1)).saveAll(invoices);
        }

        @Test
        @DisplayName("Deve marcar como excluída a fatura que chegar a zero")
        void shouldSoftDeleteInvoiceThatReachesZero() {
            Invoice zeroed = invoice("50.00", false);
            Invoice remaining = invoice("120.00", false);

            invoiceAmountService.removeBalanceAfterDeletion(List.of(zeroed, remaining), new BigDecimal("50.00"));

            assertEquals(0, zeroed.getTotalAmount().compareTo(BigDecimal.ZERO));
            assertTrue(zeroed.isDeleted());
            assertEquals(new BigDecimal("70.00"), remaining.getTotalAmount());
            assertFalse(remaining.isDeleted());
        }

        @Test
        @DisplayName("Deve marcar como excluída a fatura zerada mesmo com escala diferente de BigDecimal.ZERO")
        void shouldSoftDeleteZeroedInvoiceRegardlessOfScale() {
            Invoice zeroed = invoice("50", false);

            invoiceAmountService.removeBalanceAfterDeletion(List.of(zeroed), new BigDecimal("50.00"));

            assertTrue(zeroed.isDeleted());
        }

        @Test
        @DisplayName("Deve salvar lista vazia sem erro")
        void shouldHandleEmptyList() {
            assertDoesNotThrow(() ->
                    invoiceAmountService.removeBalanceAfterDeletion(Collections.emptyList(), BigDecimal.TEN)
            );

            verify(invoiceRepository, times(1)).saveAll(Collections.emptyList());
        }
    }

    @Nested
    @DisplayName("Cenários do addBalanceAfterRestore")
    class AddBalanceTests {

        @Test
        @DisplayName("Deve somar o valor, reativar as faturas e salvar todas")
        void shouldAddAmountAndRestoreInvoices() {
            Invoice deleted = invoice("0.00", true);
            Invoice active = invoice("40.00", false);
            List<Invoice> invoices = List.of(deleted, active);

            invoiceAmountService.addBalanceAfterRestore(invoices, new BigDecimal("30.00"));

            assertEquals(new BigDecimal("30.00"), deleted.getTotalAmount());
            assertFalse(deleted.isDeleted());
            assertEquals(new BigDecimal("70.00"), active.getTotalAmount());
            assertFalse(active.isDeleted());
            verify(invoiceRepository, times(1)).saveAll(invoices);
        }
    }
}
