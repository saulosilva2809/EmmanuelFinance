package com.emmanuelfinance.creditcard.invoice.repositories;

import com.emmanuelfinance.creditcard.invoice.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {
    List<InvoiceItem> findByTransactionId(UUID transactionId);
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE InvoiceItem i
        SET i.deleted = true
        WHERE i.transactionId = :transactionId
    """)
    void deleteByTransactionId(@Param("transactionId") UUID transactionId);
}