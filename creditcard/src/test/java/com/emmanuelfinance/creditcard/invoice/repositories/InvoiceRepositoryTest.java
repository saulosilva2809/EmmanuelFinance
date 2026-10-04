package com.emmanuelfinance.creditcard.invoice.repositories;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
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
public class InvoiceRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    private Invoice save(UUID creditCardId, int month, int year, boolean deleted) {
        return invoiceRepository.saveAndFlush(InvoiceTestDataBuilder.invoiceEntity(
                UUID.randomUUID(), creditCardId, month, year, new BigDecimal("100.00"), InvoiceStatusEnum.OPEN, deleted
        ));
    }

    @Nested
    @DisplayName("Cenários do existsByIdAndDeletedFalse")
    class ExistsByIdAndDeletedFalseTests {

        @Test
        @DisplayName("Deve retornar true quando a fatura existir e não estiver excluída")
        void shouldReturnTrueWhenInvoiceIsActive() {
            Invoice saved = save(UUID.randomUUID(), 10, 2026, false);

            assertTrue(invoiceRepository.existsByIdAndDeletedFalse(saved.getId()));
        }

        @Test
        @DisplayName("Deve retornar false quando a fatura estiver excluída")
        void shouldReturnFalseWhenInvoiceIsDeleted() {
            Invoice saved = save(UUID.randomUUID(), 10, 2026, true);

            assertFalse(invoiceRepository.existsByIdAndDeletedFalse(saved.getId()));
        }

        @Test
        @DisplayName("Deve retornar false quando o ID não existir")
        void shouldReturnFalseWhenInvoiceDoesNotExist() {
            assertFalse(invoiceRepository.existsByIdAndDeletedFalse(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("Cenários do findByCreditCardIdAndMonthAndYearAndDeletedFalse")
    class FindByCardMonthYearTests {

        @Test
        @DisplayName("Deve encontrar a fatura ativa do cartão no mês e ano")
        void shouldFindActiveInvoice() {
            UUID creditCardId = UUID.randomUUID();
            Invoice saved = save(creditCardId, 10, 2026, false);

            Optional<Invoice> result = invoiceRepository
                    .findByCreditCardIdAndMonthAndYearAndDeletedFalse(creditCardId, 10, 2026);

            assertTrue(result.isPresent());
            assertEquals(saved.getId(), result.get().getId());
        }

        @Test
        @DisplayName("Deve ignorar a fatura excluída")
        void shouldIgnoreDeletedInvoice() {
            UUID creditCardId = UUID.randomUUID();
            save(creditCardId, 10, 2026, true);

            assertTrue(invoiceRepository
                    .findByCreditCardIdAndMonthAndYearAndDeletedFalse(creditCardId, 10, 2026).isEmpty());
        }

        @Test
        @DisplayName("Deve retornar a fatura ativa quando existir uma excluída de outro mês")
        void shouldReturnOnlyMatchingMonth() {
            UUID creditCardId = UUID.randomUUID();
            save(creditCardId, 9, 2026, true);
            Invoice active = save(creditCardId, 10, 2026, false);

            Optional<Invoice> result = invoiceRepository
                    .findByCreditCardIdAndMonthAndYearAndDeletedFalse(creditCardId, 10, 2026);

            assertEquals(active.getId(), result.orElseThrow().getId());
        }
    }
}
