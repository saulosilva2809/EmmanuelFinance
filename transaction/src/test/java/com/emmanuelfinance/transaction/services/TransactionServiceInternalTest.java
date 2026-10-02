package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceInternalTest {

    @Mock
    private TransactionSelector transactionSelector;

    @Mock
    private CategoryClientCacheService categoryClientCacheService;

    @InjectMocks
    private TransactionServiceInternal transactionServiceInternal;

    @Test
    @DisplayName("Deve montar o resumo da transação com a categoria")
    void shouldBuildSummary() {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());
        CategorySummaryDTO category = TransactionTestDataBuilder.categorySummaryDTO(transaction.getCategoryId());

        when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);
        when(categoryClientCacheService.getCategorySummaryDTO(transaction.getCategoryId())).thenReturn(category);

        TransactionSummaryDTO result = transactionServiceInternal.getSummaryDTO(transaction.getId());

        assertEquals(category, result.category());
        assertEquals(transaction.getAmount(), result.amount());
        assertEquals(transaction.getStatus(), result.status());
        assertEquals(transaction.getType(), result.type());
    }

    @Test
    @DisplayName("Deve propagar TransactionNotFound quando a transação não existir")
    void shouldPropagateTransactionNotFound() {
        UUID id = UUID.randomUUID();
        when(transactionSelector.getTransactionByIdInternal(id)).thenThrow(new TransactionNotFound());

        assertThrows(TransactionNotFound.class, () -> transactionServiceInternal.getSummaryDTO(id));

        verifyNoInteractions(categoryClientCacheService);
    }
}
