package com.emmanuelfinance.auth.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResponseUserDTO (
        UUID id,
        String firstName,
        String lastname,
        String email,
        LocalDateTime created_at,
        LocalDateTime updated_at
) {}
