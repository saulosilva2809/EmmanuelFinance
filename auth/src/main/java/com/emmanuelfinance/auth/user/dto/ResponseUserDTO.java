package com.emmanuelfinance.auth.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseUserDTO (
        UUID id,
        String firstName,
        String lastName,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
