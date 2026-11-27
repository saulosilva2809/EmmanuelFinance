package com.emmanuelfinance.creditcard.invoice.repositories;

import com.emmanuelfinance.shared.entity.BaseEntity;
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
        return invoiceRepository.saveAndFlush(unsaved(InvoiceTestDataBuilder.invoiceEntity(
                UUID.randomUUID(), creditCardId, month, year, new BigDecimal("100.00"), InvoiceStatusEnum.OPEN, deleted
        )));
    }

    @Nested
    @DisplayName("Cenários do findByCreditCardIdAndMonthAndYear")
    class FindByCardMonthYearTests {

        @Test
        @DisplayName("Deve encontrar a fatura ativa do cartão no mês e ano")
        void shouldFindActiveInvoice() {
            UUID creditCardId = UUID.randomUUID();
            Invoice saved = save(creditCardId, 10, 2026, false);

            Optional<Invoice> result = invoiceRepository
                    .findByCreditCardIdAndMonthAndYear(creditCardId, 10, 2026);

            assertTrue(result.isPresent());
            assertEquals(saved.getId(), result.get().getId());
        }

        @Test
        @DisplayName("Deve encontrar também a fatura excluída, para ela ser reaproveitada")
        void shouldFindDeletedInvoice() {
            UUID creditCardId = UUID.randomUUID();
            Invoice deleted = save(creditCardId, 10, 2026, true);

            assertEquals(deleted.getId(), invoiceRepository
                    .findByCreditCardIdAndMonthAndYear(creditCardId, 10, 2026).orElseThrow().getId());
        }

        @Test
        @DisplayName("Deve retornar a fatura ativa quando existir uma excluída de outro mês")
        void shouldReturnOnlyMatchingMonth() {
            UUID creditCardId = UUID.randomUUID();
            save(creditCardId, 9, 2026, true);
            Invoice active = save(creditCardId, 10, 2026, false);

            Optional<Invoice> result = invoiceRepository
                    .findByCreditCardIdAndMonthAndYear(creditCardId, 10, 2026);

            assertEquals(active.getId(), result.orElseThrow().getId());
        }
    }

    @Nested
    @DisplayName("Cenários do findByIdAndDeletedFalse")
    class FindByIdAndDeletedFalseTests {

        @Test
        @DisplayName("Deve encontrar a fatura ativa")
        void shouldFindActiveInvoice() {
            Invoice saved = save(UUID.randomUUID(), 10, 2026, false);

            assertEquals(saved.getId(), invoiceRepository.findByIdAndDeletedFalse(saved.getId()).orElseThrow().getId());
        }

        @Test
        @DisplayName("Deve ignorar a fatura excluída")
        void shouldIgnoreDeletedInvoice() {
            Invoice saved = save(UUID.randomUUID(), 10, 2026, true);

            assertTrue(invoiceRepository.findByIdAndDeletedFalse(saved.getId()).isEmpty());
        }

        @Test
        @DisplayName("Deve persistir o valor pago com padrão zero")
        void shouldPersistAmountPaidWithZeroDefault() {
            Invoice saved = save(UUID.randomUUID(), 10, 2026, false);

            assertEquals(0, BigDecimal.ZERO.compareTo(
                    invoiceRepository.findById(saved.getId()).orElseThrow().getAmountPaid()
            ));
        }
    }

    private static <T extends BaseEntity> T unsaved(T entity) {
        entity.setId(null);
        return entity;
    }
}
