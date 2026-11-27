package com.emmanuelfinance.shared.modules.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoicePaymentFailedEvent(
        UUID eventId,
        UUID creditCardId,
        UUID invoiceId,
        BigDecimal amount
) {}
