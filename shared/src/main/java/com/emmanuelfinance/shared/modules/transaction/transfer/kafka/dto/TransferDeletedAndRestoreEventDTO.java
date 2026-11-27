package com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferDeletedAndRestoreEventDTO(
        UUID transferId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        UUID eventId
) implements TransferEvent {}
