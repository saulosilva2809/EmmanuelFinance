package com.emmanuelfinance.transaction.transaction;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaction t where t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") UUID id);

    List<Transaction> findByStatusAndDateAfter(
            StatusTransactionEnum status,
            LocalDateTime date
    );

    List<Transaction> findByStatusAndDateBefore(
            StatusTransactionEnum status,
            LocalDateTime date
    );
}
