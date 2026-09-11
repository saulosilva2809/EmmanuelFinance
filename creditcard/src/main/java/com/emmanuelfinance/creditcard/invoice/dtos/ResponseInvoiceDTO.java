package com.emmanuelfinance.creditcard.invoice.dtos;

import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseInvoiceDTO(
        UUID id,
        CreditCardSummaryDTO creditCard,
        Integer month,
        Integer year,
        Integer dueDate,
        Integer closingDate,
        BigDecimal totalAmount,
        InvoiceStatusEnum status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
