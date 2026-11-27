package com.emmanuelfinance.transaction.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID>, JpaSpecificationExecutor<Transfer> {

    Optional<Transfer> findByIdAndUserIdAndDeletedFalse(UUID transferId, UUID userId);
    Optional<Transfer> findByIdAndUserId(UUID transferId, UUID userId);
}
