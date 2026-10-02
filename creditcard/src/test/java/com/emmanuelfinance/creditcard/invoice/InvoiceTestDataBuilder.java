package com.emmanuelfinance.creditcard.invoice;

import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceItemDTO;
import com.emmanuelfinance.creditcard.invoice.dtos.ResponseInvoiceSummaryDTO;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class InvoiceTestDataBuilder {

    public static final int DUE_DATE = 24;
    public static final int CLOSING_DATE = 17;

    public static Invoice invoiceEntity(
            UUID userId,
            UUID creditCardId,
            int month,
            int year,
            BigDecimal totalAmount,
            InvoiceStatusEnum status,
            boolean deleted
    ) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setUserId(userId);
        invoice.setCreditCardId(creditCardId);
        invoice.setMonth(month);
        invoice.setYear(year);
        invoice.setDueDate(DUE_DATE);
        invoice.setClosingDate(CLOSING_DATE);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(status);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setUpdatedAt(null);
        invoice.setDeleted(deleted);
        return invoice;
    }

    public static Invoice invoiceEntity(UUID userId, UUID creditCardId) {
        return invoiceEntity(userId, creditCardId, 10, 2026, new BigDecimal("100.00"), InvoiceStatusEnum.OPEN, false);
    }

    public static Invoice invoiceEntity(UUID userId, UUID creditCardId, int month, int year) {
        return invoiceEntity(userId, creditCardId, month, year, new BigDecimal("100.00"), InvoiceStatusEnum.OPEN, false);
    }

    public static InvoiceItem invoiceItemEntity(
            UUID userId,
            UUID invoiceId,
            UUID transactionId,
            int installmentNumber,
            int totalInstallments,
            BigDecimal amount,
            boolean deleted
    ) {
        InvoiceItem item = new InvoiceItem();
        item.setId(UUID.randomUUID());
        item.setUserId(userId);
        item.setInvoiceId(invoiceId);
        item.setTransactionId(transactionId);
        item.setInstallmentNumber(installmentNumber);
        item.setTotalInstallments(totalInstallments);
        item.setAmount(amount);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(null);
        item.setDeleted(deleted);
        return item;
    }

    public static InvoiceItem invoiceItemEntity(UUID userId, UUID invoiceId, UUID transactionId) {
        return invoiceItemEntity(userId, invoiceId, transactionId, 1, 1, new BigDecimal("100.00"), false);
    }

    public static TransactionCreatedEvent transactionCreatedEvent(
            UUID userId,
            UUID creditCardId,
            BigDecimal amount,
            Integer installmentsCount,
            LocalDateTime date
    ) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                null,
                creditCardId,
                userId,
                amount,
                installmentsCount,
                TypeEnum.EXPENSE,
                StatusTransactionEnum.PENDING,
                date
        );
    }

    public static TransactionDeletedAndRestoreEvent transactionDeletedAndRestoreEvent(UUID transactionId) {
        return new TransactionDeletedAndRestoreEvent(
                transactionId,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                1,
                TypeEnum.EXPENSE,
                StatusTransactionEnum.PENDING,
                LocalDateTime.now()
        );
    }

    public static CreditCardInternalSummaryDTO creditCardInternalSummaryDTO(UUID creditCardId) {
        return new CreditCardInternalSummaryDTO(
                creditCardId,
                UUID.randomUUID(),
                new BigDecimal("10000.00"),
                DUE_DATE,
                CLOSING_DATE
        );
    }

    public static CreditCardSummaryDTO creditCardSummaryDTO(UUID creditCardId) {
        return new CreditCardSummaryDTO(creditCardId, "Cartão de Crédito C6", false);
    }

    public static TransactionSummaryDTO transactionSummaryDTO() {
        return new TransactionSummaryDTO(
                new CategorySummaryDTO(UUID.randomUUID(), "Alimentação", false),
                new BigDecimal("100.00"),
                StatusTransactionEnum.PENDING,
                TypeEnum.EXPENSE
        );
    }

    public static ResponseInvoiceDTO responseInvoiceDTO(Invoice invoice, CreditCardSummaryDTO creditCard) {
        return new ResponseInvoiceDTO(
                invoice.getId(),
                creditCard,
                invoice.getMonth(),
                invoice.getYear(),
                invoice.getDueDate(),
                invoice.getClosingDate(),
                invoice.getTotalAmount(),
                invoice.getStatus(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    public static ResponseInvoiceSummaryDTO responseInvoiceSummaryDTO(Invoice invoice) {
        return new ResponseInvoiceSummaryDTO(
                invoice.getId(),
                invoice.getMonth(),
                invoice.getYear(),
                invoice.getTotalAmount(),
                invoice.getStatus()
        );
    }

    public static ResponseInvoiceItemDTO responseInvoiceItemDTO(
            InvoiceItem item,
            ResponseInvoiceSummaryDTO invoice,
            TransactionSummaryDTO transaction
    ) {
        return new ResponseInvoiceItemDTO(
                invoice,
                transaction,
                item.getInstallmentNumber(),
                item.getTotalInstallments(),
                item.getAmount()
        );
    }
}
