package com.emmanuelfinance.shared.kafka.account;

import com.emmanuelfinance.shared.kafka.account.enums.StatusEventEnum;

import java.util.UUID;

public record AccountEventDTO(
        UUID accountId,
        UUID userId,
        StatusEventEnum status
) {}
