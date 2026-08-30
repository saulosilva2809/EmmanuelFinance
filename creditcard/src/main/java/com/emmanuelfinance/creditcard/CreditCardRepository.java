package com.emmanuelfinance.creditcard;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID>, JpaSpecificationExecutor<CreditCard> {
    Optional<CreditCard> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
    Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId);
    List<CreditCard> findByAccountId(UUID accountId);
}
