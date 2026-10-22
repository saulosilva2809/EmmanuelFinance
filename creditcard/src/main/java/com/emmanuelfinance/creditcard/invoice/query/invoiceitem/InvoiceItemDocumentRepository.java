package com.emmanuelfinance.creditcard.invoice.query.invoiceitem;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface InvoiceItemDocumentRepository extends MongoRepository<InvoiceItemDocument, UUID> {
}
