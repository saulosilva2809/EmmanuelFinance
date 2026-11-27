package com.emmanuelfinance.creditcard.invoice.selectors;

import com.emmanuelfinance.shared.entity.BaseEntity;
import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceDomainException;
import com.emmanuelfinance.creditcard.invoice.exceptions.InvoiceErrorCode;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class InvoiceSelectorTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    private InvoiceSelector invoiceSelector;

    @BeforeEach
    void setUp() {
        invoiceSelector = new InvoiceSelector(invoiceRepository);
    }

    @Nested
    @DisplayName("Cenários do getByCreditCardAndMonthAndYear")
    class GetByCreditCardAndMonthAndYearTests {

        @Test
        @DisplayName("Deve encontrar a fatura do cartão no mês e ano")
        void shouldFindInvoiceByCardMonthAndYear() {
            UUID creditCardId = UUID.randomUUID();
            Invoice saved = invoiceRepository.saveAndFlush(unsaved(
                    InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), creditCardId, 10, 2026)
            ));

            Optional<Invoice> result = invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 10, 2026);

            assertTrue(result.isPresent());
            assertEquals(saved.getId(), result.get().getId());
            assertEquals(10, result.get().getMonth());
            assertEquals(2026, result.get().getYear());
        }

        @Test
        @DisplayName("Deve retornar vazio quando mês, ano ou cartão forem diferentes")
        void shouldReturnEmptyWhenNoMatch() {
            UUID creditCardId = UUID.randomUUID();
            invoiceRepository.saveAndFlush(unsaved(
                    InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), creditCardId, 10, 2026)
            ));

            assertTrue(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 11, 2026).isEmpty());
            assertTrue(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 10, 2027).isEmpty());
            assertTrue(invoiceSelector.getByCreditCardAndMonthAndYear(UUID.randomUUID(), 10, 2026).isEmpty());
        }

        @Test
        @DisplayName("Deve devolver a fatura excluída, para ela ser reaproveitada")
        void shouldReturnDeletedInvoice() {
            UUID creditCardId = UUID.randomUUID();
            Invoice deleted = InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), creditCardId, 10, 2026);
            deleted.setDeleted(true);
            invoiceRepository.saveAndFlush(unsaved(deleted));

            assertTrue(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 10, 2026).isPresent());
        }

    }

    @Nested
    @DisplayName("Cenários do getByIdExcludingDeleted")
    class GetByIdExcludingDeletedTests {

        @Test
        @DisplayName("Deve retornar a fatura não excluída")
        void shouldReturnActiveInvoice() {
            Invoice saved = invoiceRepository.saveAndFlush(unsaved(
                    InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), UUID.randomUUID())
            ));

            assertEquals(saved.getId(), invoiceSelector.getByIdExcludingDeleted(saved.getId()).getId());
        }

        @Test
        @DisplayName("Deve lançar INVOICE_NOT_FOUND quando a fatura estiver excluída")
        void shouldThrowWhenInvoiceIsDeleted() {
            Invoice saved = invoiceRepository.saveAndFlush(unsaved(InvoiceTestDataBuilder.invoiceEntity(
                    UUID.randomUUID(), UUID.randomUUID(), 10, 2026, new BigDecimal("100.00"), InvoiceStatusEnum.OPEN, true
            )));

            InvoiceDomainException exception = assertThrows(InvoiceDomainException.class,
                    () -> invoiceSelector.getByIdExcludingDeleted(saved.getId()));

            assertEquals(InvoiceErrorCode.INVOICE_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar INVOICE_NOT_FOUND quando o ID não existir")
        void shouldThrowWhenInvoiceDoesNotExist() {
            assertThrows(InvoiceDomainException.class,
                    () -> invoiceSelector.getByIdExcludingDeleted(UUID.randomUUID()));
        }
    }

    private static <T extends BaseEntity> T unsaved(T entity) {
        entity.setId(null);
        return entity;
    }
}
