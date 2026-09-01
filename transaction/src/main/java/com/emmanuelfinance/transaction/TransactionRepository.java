package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
    Optional<Transaction> findByIdAndDeletedFalse(UUID id);
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
    List<Transaction> findByStatusAndDateAfter(
            StatusTransactionEnum status,
            LocalDateTime date
    );

    List<Transaction> findByStatusAndDateBefore(
            StatusTransactionEnum status,
            LocalDateTime date
    );
}
