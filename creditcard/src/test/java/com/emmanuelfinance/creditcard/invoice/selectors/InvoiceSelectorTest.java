package com.emmanuelfinance.creditcard.invoice.selectors;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

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
            Invoice saved = invoiceRepository.saveAndFlush(
                    InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), creditCardId, 10, 2026)
            );

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
            invoiceRepository.saveAndFlush(
                    InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), creditCardId, 10, 2026)
            );

            assertTrue(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 11, 2026).isEmpty());
            assertTrue(invoiceSelector.getByCreditCardAndMonthAndYear(creditCardId, 10, 2027).isEmpty());
            assertTrue(invoiceSelector.getByCreditCardAndMonthAndYear(UUID.randomUUID(), 10, 2026).isEmpty());
        }

    }

    @Nested
    @DisplayName("Cenários do getById")
    class GetByIdTests {

        @Test
        @DisplayName("Deve retornar a fatura pelo ID")
        void shouldReturnInvoiceById() {
            Invoice saved = invoiceRepository.saveAndFlush(
                    InvoiceTestDataBuilder.invoiceEntity(UUID.randomUUID(), UUID.randomUUID())
            );

            Invoice result = invoiceSelector.getById(saved.getId());

            assertNotNull(result);
            assertEquals(saved.getId(), result.getId());
            assertEquals(saved.getTotalAmount(), result.getTotalAmount());
        }
    }
}
