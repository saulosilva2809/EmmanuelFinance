package com.emmanuelfinance.shared.dto;

import java.io.Serializable;
import java.util.UUID;

public record AccountSummaryDTO (
        UUID id,
        String name
) implements Serializable {}
