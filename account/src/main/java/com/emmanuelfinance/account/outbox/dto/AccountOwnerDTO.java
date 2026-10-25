package com.emmanuelfinance.account.outbox.dto;

import java.util.UUID;

// conteúdo das linhas ACCOUNT_OWNER_CACHED e ACCOUNT_OWNER_REMOVED: de quem é a conta, para gravar ou tirar do cache (Redis)
public record AccountOwnerDTO(UUID accountId, UUID userId) {}
