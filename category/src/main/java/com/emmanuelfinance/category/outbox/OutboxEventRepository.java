package com.emmanuelfinance.category.outbox;

import com.emmanuelfinance.shared.outbox.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends BaseOutboxEventRepository<OutboxEvent> {
}
