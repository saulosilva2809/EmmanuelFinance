package com.emmanuelfinance.creditcard;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID>, JpaSpecificationExecutor<CreditCard> {
}
