package com.emmanuelfinance.creditcard.payment.outbox;

import com.emmanuelfinance.shared.outbox.OutboxRelay;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// Liga o relay do Outbox neste serviço. Fica aqui, e não na classe Application, para que os testes de
// fatia (@WebMvcTest) não tentem criar o relay, que precisa do repositório do Outbox.
@Configuration
@Import(OutboxRelay.class)
public class OutboxConfig {
}
