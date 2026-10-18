package com.emmanuelfinance.account.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountDocumentRepository extends MongoRepository<AccountDocument, UUID> {

    Optional<AccountDocument> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
