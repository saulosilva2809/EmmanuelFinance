package com.emmanuelfinance.account;

import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    Optional<Account> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    @Query(value = "SELECT * FROM account WHERE id = :id AND user_id = :userId", nativeQuery = true)
    Optional<Account> findByIdAndUserIdIncludingDeleted(@Param("id") UUID id, @org.springframework.data.repository.query.Param("userId") UUID userId);
}