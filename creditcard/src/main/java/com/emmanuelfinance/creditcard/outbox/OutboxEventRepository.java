package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.shared.outbox.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends BaseOutboxEventRepository<OutboxEvent> {
}
