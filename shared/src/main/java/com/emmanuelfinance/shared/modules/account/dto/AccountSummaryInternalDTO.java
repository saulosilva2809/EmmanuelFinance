package com.emmanuelfinance.shared.modules.account.dto;

import com.emmanuelfinance.shared.enums.BanksEnum;

import java.io.Serializable;
import java.util.UUID;

public record AccountSummaryInternalDTO(
        UUID id,
        String name,
        BanksEnum bank,
        boolean deleted
) implements Serializable {}
