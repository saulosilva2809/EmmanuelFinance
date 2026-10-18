package com.emmanuelfinance.account.outbox.dto;

import java.util.UUID;

// conteúdo da linha ACCOUNT_OWNER_CACHED: de quem é a conta, para gravar no cache (Redis)
public record AccountOwnerDTO(UUID accountId, UUID userId) {}
