package com.emmanuelfinance.creditcard.invoice.repositories;

import com.emmanuelfinance.creditcard.invoice.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByIdAndDeletedFalse(UUID invoiceId);
    Optional<Invoice> findByCreditCardIdAndMonthAndYearAndDeletedFalse(UUID creditCardId, Integer month, Integer year);
    boolean existsByIdAndDeletedFalse(UUID id);
}