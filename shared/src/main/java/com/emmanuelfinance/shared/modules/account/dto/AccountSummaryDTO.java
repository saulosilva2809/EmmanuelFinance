package com.emmanuelfinance.shared.modules.account.dto;

import java.io.Serializable;
import java.util.UUID;

public record AccountSummaryDTO (
        UUID id,
        String name
) implements Serializable {}
