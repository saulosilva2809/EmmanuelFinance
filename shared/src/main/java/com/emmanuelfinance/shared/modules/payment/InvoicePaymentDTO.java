package com.emmanuelfinance.shared.modules.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoicePaymentDTO(
        UUID accountId,
        UUID userId,
        UUID creditCardId,
        String creditCardName,
        UUID invoiceId,
        int month,
        int year,
        BigDecimal totalPaid
) {}
