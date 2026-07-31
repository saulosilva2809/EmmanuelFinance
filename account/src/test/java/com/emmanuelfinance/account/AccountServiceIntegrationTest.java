package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.kafka.producer.AccountEventPublisher;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.kafka.account.enums.StatusEventEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
    private Jwt jwt;

    private UUID accountId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        Account account = new Account();
        account.setUserId(userId);
        account.setName("Conta Antiga");
        account.setType(TypeEnum.CHECKING);
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));

        Account saved = accountRespository.save(account);
        accountId = saved.getId();

        // configura o mock so serviço externo
        when(userClientCacheService.getUserById(userId))
                .thenReturn(new UserSummaryDTO(userId, "saulo@gmail.com"));
    }

    @Test
    void shouldCreateAccountAndPublishEventSuccessfully() {
        UUID userId = UUID.randomUUID();
        when(jwt.getSubject()).thenReturn(userId.toString());

        CreateAccountDTO accountDTO = new CreateAccountDTO(
                "Conta Itaú",
                TypeEnum.CHECKING,
                new BigDecimal(BigInteger.ZERO)
        );

        ResponseAccountDTO response = accountService.create(jwt, accountDTO);

        assertNotNull(response);
        assertEquals(response.name(), accountDTO.name());

        Optional<Account> savedAccountInDb = accountRespository.findById(response.id());
        assertNotNull(savedAccountInDb);
        assertEquals(savedAccountInDb.get().getId(), response.id());

        verify(accountEventPublisher, times(1)).publishAccountCreate(
                new AccountEventDTO(response.id(), StatusEventEnum.CREATED)
        );
    }

    @Test
    void shouldUpdateAccountAndEvictCache() {
        cacheManager.getCache("accounts").put(accountId, "dados antigos em cache");

        // garante que o cache realmente existe antes do update
        assertNotNull(cacheManager.getCache("accounts").get(accountId));

        UpdateAccountDTO updateDTO = new UpdateAccountDTO(
                "Conta Nova",
                TypeEnum.INVESTMENT
        );

        // when
        ResponseAccountDTO updateResponse = accountService.update(accountId, updateDTO);

        // then
        assertNotNull(updateResponse);
        assertEquals("Conta Nova", updateResponse.name());

        // valida o @transactional no banco H2
        Account accountInDb = accountRespository.findById(accountId).orElseThrow();
        assertEquals("Conta Nova", accountInDb.getName());
        assertEquals(TypeEnum.INVESTMENT, accountInDb.getType());

        // valida o cache
        assertNull(cacheManager.getCache("accounts").get(accountId));
    }
}

