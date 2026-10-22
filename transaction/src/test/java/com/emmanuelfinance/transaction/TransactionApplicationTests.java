package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.outbox.OutboxRelay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class TransactionApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	// sem o relay o Outbox grava mas nunca entrega nada, e nenhum erro aparece
	@Test
	void outboxRelayIsRegistered() {
		assertEquals(1, applicationContext.getBeansOfType(OutboxRelay.class).size());
	}

}
