package com.emmanuelfinance.shared.modules.account.kafka.account;

import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;

import java.util.UUID;

public record AccountEventDTO(
        UUID accountId,
        UUID userId,
        StatusEventEnum status
) {}
