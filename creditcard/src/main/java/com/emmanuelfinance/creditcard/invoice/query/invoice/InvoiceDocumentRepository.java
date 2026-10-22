package com.emmanuelfinance.creditcard.invoice.query.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceDocumentRepository extends MongoRepository<InvoiceDocument, UUID> {

    Optional<InvoiceDocument> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
