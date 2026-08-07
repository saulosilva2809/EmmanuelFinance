package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.kafka.producer.AccountEventPublisher;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.kafka.account.enums.StatusEventEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRespository accountRespository;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private UserClientCacheService userClientCacheService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AccountEventPublisher accountEventPublisher;

    @MockBean
    private SecurityUtils securityUtils;

    private UUID accountId;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        // Autentica o usuário no contexto do Spring Security para o teste
        mockAuthenticatedUser(userId);

        Account account = new Account();
        account.setUserId(userId);
        account.setName("Conta Antiga");
        account.setType(TypeEnum.CHECKING);
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));

        Account saved = accountRespository.save(account);
        accountId = saved.getId();

        when(userClientCacheService.getUserById(userId))
                .thenReturn(new UserSummaryDTO(userId, "saulo@gmail.com"));
    }

    @AfterEach
    void tearDown() {
        // Limpa o contexto de segurança após cada teste
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(UUID userId) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void shouldCreateAccountAndPublishEventSuccessfully() {
        CreateAccountDTO accountDTO = new CreateAccountDTO(
                "Conta Itaú",
                TypeEnum.CHECKING,
                new BigDecimal(BigInteger.ZERO)
        );

        ResponseAccountDTO response = accountService.create(accountDTO);

        assertNotNull(response);
        assertEquals(response.name(), accountDTO.name());

        Optional<Account> savedAccountInDb = accountRespository.findById(response.id());
        assertTrue(savedAccountInDb.isPresent());
        assertEquals(savedAccountInDb.get().getId(), response.id());

        verify(accountEventPublisher, times(1)).publishAccountCreate(
                any(AccountEventDTO.class)
        );
    }

    @Test
    void shouldUpdateAccountAndEvictCache() {
        if (cacheManager.getCache("accounts") != null) {
            cacheManager.getCache("accounts").put(accountId, "dados antigos em cache");
            assertNotNull(cacheManager.getCache("accounts").get(accountId));
        }

        UpdateAccountDTO updateDTO = new UpdateAccountDTO(
                "Conta Nova",
                TypeEnum.INVESTMENT
        );

        ResponseAccountDTO updateResponse = accountService.update(accountId, updateDTO);

        assertNotNull(updateResponse);
        assertEquals("Conta Nova", updateResponse.name());

        Account accountInDb = accountRespository.findById(accountId).orElseThrow();
        assertEquals("Conta Nova", accountInDb.getName());
        assertEquals(TypeEnum.INVESTMENT, accountInDb.getType());

        if (cacheManager.getCache("accounts") != null) {
            assertNull(cacheManager.getCache("accounts").get(accountId));
        }
    }
}