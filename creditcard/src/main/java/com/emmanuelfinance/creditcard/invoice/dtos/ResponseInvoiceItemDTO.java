package com.emmanuelfinance.creditcard.invoice.dtos;

import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;

import java.math.BigDecimal;

public record ResponseInvoiceItemDTO (
        ResponseInvoiceSummaryDTO invoice,
        TransactionSummaryDTO transaction,
        Integer installmentNumber,
        Integer totalInstallments,
        BigDecimal amount
) {}