package com.emmanuelfinance.creditcard.creditcard.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditCardDocumentRepository extends MongoRepository<CreditCardDocument, UUID> {

    Optional<CreditCardDocument> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
