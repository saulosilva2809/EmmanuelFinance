package com.emmanuelfinance.creditcard.invoice.repositories;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {
    List<InvoiceItem> findByTransactionId(UUID transactionId);
    void deleteByTransactionId(UUID transactionId);
}