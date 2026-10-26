package com.emmanuelfinance.shared.modules.account.dto;

import com.emmanuelfinance.shared.modules.account.enums.StatusEventEnum;

import java.util.UUID;

public record AccountEventDTO(
        UUID accountId,
        UUID userId,
        StatusEventEnum status
) {}
