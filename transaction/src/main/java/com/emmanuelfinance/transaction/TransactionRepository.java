package com.emmanuelfinance.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByIdempotencyKey(String idempotencyKey);
}
