package com.emmanuelfinance.creditcard.invoice.dtos;

import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;

import java.math.BigDecimal;

public record ResponseInvoiceSummaryDTO(
        Integer month,
        Integer year,
        BigDecimal totalAmount,
        InvoiceStatusEnum status
) {
}
