package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.exceptions.TransactionNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionSelector {

    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;

    public Transaction getTransactionById(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserIdAndDeletedFalse(
                id,
                userId
        ).orElseThrow(() -> new TransactionNotFound());

        return transaction;
    }

    public Transaction getTransactionByIdIncluingDeleted(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        Transaction transaction = transactionRepository.findByIdAndUserId(
                id,
                userId
        ).orElseThrow(() -> new TransactionNotFound());

        return transaction;
    }

    public Transaction getTransactionByIdInternal(UUID id) {
        Transaction transaction = transactionRepository.findById(
                id
        ).orElseThrow(TransactionNotFound::new);

        return transaction;
    }

    public List<Transaction> getPendingTransactionsAfter(LocalDateTime date) {
        return transactionRepository.findByStatusAndDateAfter(
                StatusTransactionEnum.PENDING,
                date
        );
    }

    public List<Transaction> getPendingTransactionsBefore(LocalDateTime date) {
        return transactionRepository.findByStatusAndDateBefore(
                StatusTransactionEnum.PENDING,
                date
        );
    }
}
