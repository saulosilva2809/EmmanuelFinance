package com.emmanuelfinance.account.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    // busca as ultimas 100 pendencias, da mais antiga ate a mais nova
    List<OutboxEvent> findTop100ByOrderByCreatedAtAsc();
}
