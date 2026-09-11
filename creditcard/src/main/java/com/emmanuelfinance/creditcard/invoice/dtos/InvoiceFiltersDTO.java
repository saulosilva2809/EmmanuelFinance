package com.emmanuelfinance.creditcard.invoice.dtos;

import com.emmanuelfinance.shared.modules.creditcard.enums.InvoiceStatusEnum;

import java.util.UUID;

public record InvoiceFiltersDTO(
        UUID creditCardId,
        Integer month,
        Integer year,
        InvoiceStatusEnum status
) {}
