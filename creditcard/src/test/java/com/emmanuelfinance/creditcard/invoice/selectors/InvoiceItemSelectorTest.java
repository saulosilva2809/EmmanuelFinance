package com.emmanuelfinance.creditcard.invoice.selectors;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.invoice.repositories.InvoiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
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
public class InvoiceItemSelectorTest {

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    private InvoiceItemSelector invoiceItemSelector;

    @BeforeEach
    void setUp() {
        invoiceItemSelector = new InvoiceItemSelector(invoiceItemRepository);
    }

    @Test
    @DisplayName("Deve retornar apenas os itens da transação informada")
    void shouldReturnOnlyItemsOfTransaction() {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        InvoiceItem item1 = InvoiceTestDataBuilder.invoiceItemEntity(
                userId, UUID.randomUUID(), transactionId, 1, 2, new BigDecimal("50.00"), false
        );
        InvoiceItem item2 = InvoiceTestDataBuilder.invoiceItemEntity(
                userId, UUID.randomUUID(), transactionId, 2, 2, new BigDecimal("50.00"), false
        );
        InvoiceItem other = InvoiceTestDataBuilder.invoiceItemEntity(userId, UUID.randomUUID(), UUID.randomUUID());
        invoiceItemRepository.saveAllAndFlush(List.of(item1, item2, other));

        List<InvoiceItem> result = invoiceItemSelector.getByTransactionId(transactionId);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(i -> i.getTransactionId().equals(transactionId)));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando a transação não tiver itens")
    void shouldReturnEmptyListWhenNoItems() {
        List<InvoiceItem> result = invoiceItemSelector.getByTransactionId(UUID.randomUUID());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
