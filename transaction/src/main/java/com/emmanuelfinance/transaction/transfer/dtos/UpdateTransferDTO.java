package com.emmanuelfinance.transaction.transfer.dtos;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateTransferDTO(
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String description
) {}