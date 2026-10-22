package com.emmanuelfinance.transaction.transaction.outbox;

import com.emmanuelfinance.shared.outbox.OutboxRelay;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(OutboxRelay.class)
public class OutboxConfig {
}
