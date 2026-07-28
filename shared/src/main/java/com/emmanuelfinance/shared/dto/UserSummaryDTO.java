package com.emmanuelfinance.shared.dto;

import java.io.Serializable;
import java.util.UUID;

public record UserSummaryDTO(
        UUID uuid,
        String email
) implements Serializable {}
