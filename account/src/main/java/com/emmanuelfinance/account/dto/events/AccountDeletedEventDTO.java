package com.emmanuelfinance.account.dto.events;

import java.util.UUID;

// publicado quando uma conta é deletada
public record AccountDeletedEventDTO(UUID accountId) {}
