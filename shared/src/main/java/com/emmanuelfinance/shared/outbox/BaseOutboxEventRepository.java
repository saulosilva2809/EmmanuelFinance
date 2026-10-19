package com.emmanuelfinance.shared.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.UUID;

@NoRepositoryBean
public interface BaseOutboxEventRepository<T extends BaseOutboxEvent> extends JpaRepository<T, UUID> {

    // busca as ultimas 100 pendencias, da mais antiga ate a mais nova
    List<T> findTop100ByOrderByCreatedAtAsc();
}
