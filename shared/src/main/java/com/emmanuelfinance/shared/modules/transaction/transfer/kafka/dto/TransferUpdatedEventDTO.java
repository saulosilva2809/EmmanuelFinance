package com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferUpdatedEventDTO(
        UUID transferId,
        UUID oldSourceAccountId,
        UUID oldDestinationAccountId,
        BigDecimal oldAmount,
        UUID newSourceAccountId,
        UUID newDestinationAccountId,
        BigDecimal newAmount,
        UUID eventId
) implements TransferEvent {}
