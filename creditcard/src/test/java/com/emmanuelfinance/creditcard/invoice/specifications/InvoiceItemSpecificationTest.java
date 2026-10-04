package com.emmanuelfinance.creditcard.invoice.specifications;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceItemRepository;
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
public class InvoiceItemSpecificationTest {

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();

    private InvoiceItem save(UUID owner, UUID invoice, boolean deleted) {
        return invoiceItemRepository.saveAndFlush(InvoiceTestDataBuilder.invoiceItemEntity(
                owner, invoice, UUID.randomUUID(), 1, 1, new BigDecimal("100.00"), deleted
        ));
    }

    private List<InvoiceItem> find(UUID owner, UUID invoice) {
        return invoiceItemRepository.findAll(InvoiceItemSpecification.withFilter(owner, invoice));
    }

    @Test
    @DisplayName("Deve retornar apenas os itens ativos da fatura e do usuário")
    void shouldReturnOnlyActiveItemsOfInvoiceAndUser() {
        InvoiceItem target = save(userId, invoiceId, false);
        save(userId, invoiceId, true);
        save(UUID.randomUUID(), invoiceId, false);
        save(userId, UUID.randomUUID(), false);

        List<InvoiceItem> result = find(userId, invoiceId);

        assertEquals(1, result.size());
        assertEquals(target.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando todos os itens estiverem excluídos")
    void shouldReturnEmptyWhenAllItemsAreDeleted() {
        save(userId, invoiceId, true);

        assertTrue(find(userId, invoiceId).isEmpty());
    }
}
