package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.kafka.AccountEventPublisher;
import com.emmanuelfinance.account.services.AccountService;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AccountIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private UserClientCacheService userClientCacheService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AccountEventPublisher accountEventPublisher;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Nested
    @DisplayName("Tests of create method")
    class CreateMethodTests {

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
    }

    @Nested
    @DisplayName("Tests of update method")
    class UpdateMethodTests {

        @Test
        void shouldUpdateAccountAndEvictCache() {
            Account account = new Account();
            account.setUserId(userId);
            account.setName("Conta Antiga");
            account.setType(TypeEnum.CHECKING);
            account.setBank(BanksEnum.C6_BANK);
            account.setInitialBalance(new BigDecimal("1000.00"));
            account.setCurrentBalance(new BigDecimal("1000.00"));

            Account savedAccount = accountRepository.saveAndFlush(account);
            UUID accountId = savedAccount.getId();

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

    @Nested
    @DisplayName("Tests of listDeleted method")
    class ListDeletedMethodTests {

        @Test
        void shouldListDeletedCards() {
            accountRepository.deleteAll();

            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountActiveEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    false
            );
            accountActiveEntity.setUserId(userId);
            accountActiveEntity.setVersion(0L);

            CreateAccountDTO accountDeletedDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountDeletedEntity = AccountTestDataBuilder.accountEntity(
                    accountDeletedDTO,
                    userId,
                    true
            );
            accountDeletedEntity.setUserId(userId);
            accountDeletedEntity.setVersion(0L);

            AccountFiltersDTO filters = new AccountFiltersDTO(
                    null,
                    null
            );

            accountRepository.saveAndFlush(accountActiveEntity);
            Account saveAndFlushdDeletedAccount = accountRepository.saveAndFlush(accountDeletedEntity);

            Pageable pageable = PageRequest.of(0, 10);

            PageResponseDTO<ResponseAccountDTO> response = accountService.listDeleted(
                    filters,
                    pageable
            );

            assertEquals(1, response.content().size());
            assertEquals(saveAndFlushdDeletedAccount.getId(), response.content().get(0).id());
        }
    }

    @Nested
    @DisplayName("Tests of list method")
    class ListMethodTests {

        @Test
        void shouldListTheActiveCards() {
            accountRepository.deleteAll();

            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountActiveEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    false
            );
            accountActiveEntity.setUserId(userId);
            accountActiveEntity.setVersion(0L);

            CreateAccountDTO accountDeletedDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountDeletedEntity = AccountTestDataBuilder.accountEntity(
                    accountDeletedDTO,
                    userId,
                    true
            );
            accountDeletedEntity.setUserId(userId);
            accountDeletedEntity.setVersion(0L);

            AccountFiltersDTO filters = new AccountFiltersDTO(
                    null,
                    null
            );

            accountRepository.saveAndFlush(accountDeletedEntity);
            Account saveAndFlushdActiveAccount = accountRepository.saveAndFlush(accountActiveEntity);

            Pageable pageable = PageRequest.of(0, 10);

            PageResponseDTO<ResponseAccountDTO> response = accountService.list(
                    filters,
                    pageable
            );

            assertEquals(1, response.content().size());
            assertEquals(saveAndFlushdActiveAccount.getId(), response.content().get(0).id());
        }
    }
}