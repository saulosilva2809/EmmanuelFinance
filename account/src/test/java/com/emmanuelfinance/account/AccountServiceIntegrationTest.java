package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.kafka.producer.AccountEventPublisher;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
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
    private AccountRepository accountRepository;

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
    private UUID userId;

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        userId = securityUtils.getCurrentUserId();

        Account account = new Account();
        account.setUserId(userId);
        account.setName("Conta Antiga");
        account.setType(TypeEnum.CHECKING);
        account.setBank(BanksEnum.C6_BANK);
        account.setInitialBalance(new BigDecimal("1000.00"));
        account.setCurrentBalance(new BigDecimal("1000.00"));

        Account saved = accountRepository.save(account);
        accountId = saved.getId();

        when(userClientCacheService.getUserById(userId))
                .thenReturn(new UserSummaryDTO(userId, "saulo@gmail.com"));
    }

    @Test
    void shouldCreateAccountAndPublishEventSuccessfully() {
        CreateAccountDTO accountDTO = new CreateAccountDTO(
                "Conta Itaú",
                TypeEnum.CHECKING,
                BanksEnum.C6_BANK,
                new BigDecimal(BigInteger.ZERO)
        );

        ResponseAccountDTO response = accountService.create(accountDTO);

        assertNotNull(response);
        assertEquals(response.name(), accountDTO.name());

        Optional<Account> savedAccountInDb = accountRepository.findByIdAndUserIdAndDeletedFalse(
                response.id(),
                userId
        );
        assertTrue(savedAccountInDb.isPresent());
        assertEquals(savedAccountInDb.get().getId(), response.id());

        verify(accountEventPublisher, times(1)).publishAccount(
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

        Account accountInDb = accountRepository.findByIdAndUserIdAndDeletedFalse(
                accountId,
                userId
        ).orElseThrow();
        assertEquals("Conta Nova", accountInDb.getName());
        assertEquals(TypeEnum.INVESTMENT, accountInDb.getType());

        if (cacheManager.getCache("accounts") != null) {
            assertNull(cacheManager.getCache("accounts").get(accountId));
        }
    }
}