package com.emmanuelfinance.creditcard.invoice.specifications;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.dtos.InvoiceFiltersDTO;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceRepository;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class InvoiceSpecificationTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID creditCardId = UUID.randomUUID();

    private Invoice save(UUID owner, UUID cardId, int month, int year, InvoiceStatusEnum status, boolean deleted) {
        return invoiceRepository.saveAndFlush(InvoiceTestDataBuilder.invoiceEntity(
                owner, cardId, month, year, new BigDecimal("100.00"), status, deleted
        ));
    }

    private List<Invoice> find(InvoiceFiltersDTO filters, UUID owner) {
        return invoiceRepository.findAll(InvoiceSpecification.withFilter(filters, owner));
    }

    private final InvoiceFiltersDTO noFilters = new InvoiceFiltersDTO(null, null, null, null);

    @Test
    @DisplayName("Deve retornar apenas as faturas do usuário")
    void shouldReturnOnlyInvoicesOfTheUser() {
        Invoice mine = save(userId, creditCardId, 10, 2026, InvoiceStatusEnum.OPEN, false);
        save(UUID.randomUUID(), creditCardId, 10, 2026, InvoiceStatusEnum.OPEN, false);

        List<Invoice> result = find(noFilters, userId);

        assertEquals(1, result.size());
        assertEquals(mine.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("Deve ignorar as faturas excluídas")
    void shouldIgnoreDeletedInvoices() {
        Invoice active = save(userId, creditCardId, 10, 2026, InvoiceStatusEnum.OPEN, false);
        save(userId, creditCardId, 11, 2026, InvoiceStatusEnum.OPEN, true);

        List<Invoice> result = find(noFilters, userId);

        assertEquals(1, result.size());
        assertEquals(active.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("Deve filtrar por cartão, mês, ano e status")
    void shouldApplyFilters() {
        Invoice target = save(userId, creditCardId, 10, 2026, InvoiceStatusEnum.PAID, false);
        save(userId, UUID.randomUUID(), 10, 2026, InvoiceStatusEnum.PAID, false);
        save(userId, creditCardId, 11, 2026, InvoiceStatusEnum.PAID, false);
        save(userId, creditCardId, 10, 2027, InvoiceStatusEnum.PAID, false);
        save(userId, creditCardId, 10, 2026, InvoiceStatusEnum.OPEN, false);

        List<Invoice> result = find(
                new InvoiceFiltersDTO(creditCardId, 10, 2026, InvoiceStatusEnum.PAID), userId
        );

        assertEquals(1, result.size());
        assertEquals(target.getId(), result.get(0).getId());
    }
}
