package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionSelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceInternal {

    private final TransactionSelector transactionSelector;
    private final CategoryClientCacheService categoryClientCacheService;

    public TransactionSummaryDTO getSummaryDTO(UUID transactionId) {
        Transaction transaction = transactionSelector.getTransactionByIdInternal(
                transactionId
        );

        CategorySummaryDTO category = categoryClientCacheService.getCategorySummaryDTO(
                transaction.getCategoryId()
        );

        return new TransactionSummaryDTO(
                category,
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getType()
        );
    }
}
