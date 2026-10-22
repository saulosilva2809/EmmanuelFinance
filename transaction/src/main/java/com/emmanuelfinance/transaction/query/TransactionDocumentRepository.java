package com.emmanuelfinance.transaction.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionDocumentRepository extends MongoRepository<TransactionDocument, UUID> {

    Optional<TransactionDocument> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
