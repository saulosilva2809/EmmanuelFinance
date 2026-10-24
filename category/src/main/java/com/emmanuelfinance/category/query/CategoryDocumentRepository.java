package com.emmanuelfinance.category.query;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryDocumentRepository extends MongoRepository<CategoryDocument, UUID> {

    Optional<CategoryDocument> findByIdAndUserIdAndDeletedFalse(UUID categoryId, UUID userId);
}
