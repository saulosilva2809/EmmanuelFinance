package com.emmanuelfinance.transaction.transfer.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferDocumentRepository extends MongoRepository<TransferDocument, UUID> {

    Optional<TransferDocument> findByIdAndUserIdAndDeletedFalse(UUID transferId, UUID userId);
}
