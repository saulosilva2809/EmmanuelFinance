package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.exceptions.TransactionNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
