package com.emmanuelfinance.account;

import com.emmanuelfinance.account.kafka.producer.AccountEventPublisher;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AccountApplicationTests {

	@MockBean
	private JwtDecoder jwtDecoder;

	@MockBean
	private UserClientCacheService userClientCacheService;

	@MockBean
	private AccountEventPublisher accountEventPublisher;

	@MockBean
	private SecurityUtils securityUtils;

	@Test
	void contextLoads() {
	}
}