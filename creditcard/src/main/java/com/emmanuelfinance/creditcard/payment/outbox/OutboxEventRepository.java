package com.emmanuelfinance.creditcard.payment.outbox;

import com.emmanuelfinance.shared.outbox.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends BaseOutboxEventRepository<OutboxEvent> {
}
