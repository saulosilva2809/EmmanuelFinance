package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.exceptions.AccountNotFound;
import com.emmanuelfinance.account.kafka.producer.AccountEventPublisher;
import com.emmanuelfinance.shared.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRespository accountRespository;

    @InjectMocks
    private AccountService accountService;

    @Mock
    private UserClientCacheService userClientCacheService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AccountEventPublisher accountEventPublisher;

    @Spy
    private AccountMapper accountMapper = Mappers.getMapper(AccountMapper.class);

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Nested
    @DisplayName("Tests of the create method")
    class CreateMethodTests {

        @Test
        void shouldCreateAccountSuccessfully() {
            doNothing().when(accountEventPublisher).publishAccountCreate(any());
            UUID generatedAccountId = UUID.randomUUID();

            UserSummaryDTO mockUser = new UserSummaryDTO(userId, "saulocomercial7@gmail.com");
            when(userClientCacheService.getUserById(userId)).thenReturn(mockUser);

            CreateAccountDTO createDTO = new CreateAccountDTO(
                    "Conta Itaú",
                    TypeEnum.CHECKING,
                    new BigDecimal("1000.00")
            );

            when(accountRespository.save(any(Account.class))).thenAnswer(invocation -> {
                Account accountToSave = invocation.getArgument(0);
                accountToSave.setId(generatedAccountId);
                return accountToSave;
            });

            ResponseAccountDTO result = accountService.create(createDTO);

            assertNotNull(result);
            assertEquals(generatedAccountId, result.id());
            assertEquals("Conta Itaú", result.name());
            assertEquals(new BigDecimal("1000.00"), result.initialBalance());
            assertEquals(new BigDecimal("1000.00"), result.currentBalance());

            verify(accountRespository, times(1)).save(argThat(
                    account -> account.getUserId().equals(userId) &&
                            account.getName().equals("Conta Itaú") &&
                            account.getInitialBalance().equals(new BigDecimal("1000.00"))
            ));

            verify(userClientCacheService, times(1)).getUserById(userId);
        }
    }

    @Nested
    @DisplayName("Tests of the view method")
    class ViewMethodTests {

        @Test
        void shouldReturnTheAccountSuccessfully() {
            UUID accountId = UUID.randomUUID();

            Account mockAccount = new Account();
            mockAccount.setId(accountId);
            mockAccount.setName("Conta Itaú");
            mockAccount.setUserId(userId);
            mockAccount.setType(TypeEnum.INVESTMENT);
            mockAccount.setInitialBalance(new BigDecimal("1000.00"));
            mockAccount.setCurrentBalance(new BigDecimal("1000.00"));

            UserSummaryDTO mockUser = new UserSummaryDTO(userId, "saulocomercial7@gmail.com");

            when(accountRespository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(mockAccount));
            when(userClientCacheService.getUserById(userId)).thenReturn(mockUser);

            ResponseAccountDTO result = accountService.view(accountId);

            assertNotNull(result);
            assertEquals(accountId, result.id());
            assertEquals("Conta Itaú", result.name());
            assertEquals(new BigDecimal("1000.00"), result.initialBalance());
            assertEquals(new BigDecimal("1000.00"), result.currentBalance());

            verify(accountRespository, times(1)).findByIdAndUserId(accountId, userId);
            verify(userClientCacheService, times(1)).getUserById(userId);
        }

        @Test
        void shouldThrowAccountNotFoundWhenAccountDoesNotExist() {
            UUID nonExistentAccountId = UUID.randomUUID();

            when(accountRespository.findByIdAndUserId(nonExistentAccountId, userId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFound.class, () -> accountService.view(nonExistentAccountId));

            verify(accountRespository, times(1)).findByIdAndUserId(nonExistentAccountId, userId);
            verifyNoInteractions(userClientCacheService);
        }
    }

    @Nested
    @DisplayName("Tests of the list method")
    class ListMethodTests {

        @Test
        void shouldReturnPaginatedAccountsSuccessfully() {
            UUID accountId = UUID.randomUUID();

            Account mockAccount = new Account();
            mockAccount.setId(accountId);
            mockAccount.setUserId(userId);
            mockAccount.setName("Conta Itaú");
            mockAccount.setType(TypeEnum.CHECKING);
            mockAccount.setInitialBalance(new BigDecimal("1000.00"));
            mockAccount.setCurrentBalance(new BigDecimal("1000.00"));

            AccountFiltersDTO filters = new AccountFiltersDTO("Itaú", TypeEnum.CHECKING);
            Pageable pageable = PageRequest.of(0, 10);
            UserSummaryDTO mockUser = new UserSummaryDTO(userId, "saulocomercial7@gmail.com");

            List<Account> accountList = List.of(mockAccount);
            Page<Account> accountPage = new PageImpl<>(accountList, pageable, accountList.size());

            when(accountRespository.findAll(any(Specification.class), eq(pageable))).thenReturn(accountPage);
            when(userClientCacheService.getUserById(userId)).thenReturn(mockUser);

            PageResponseDTO<ResponseAccountDTO> result = accountService.list(filters, pageable);

            assertNotNull(result);
            assertNotNull(result.content());
            assertEquals(1, result.content().size());
            assertEquals(accountId, result.content().get(0).id());
            assertEquals("Conta Itaú", result.content().get(0).name());

            verify(accountRespository, times(1)).findAll(any(Specification.class), eq(pageable));
            verify(userClientCacheService, times(1)).getUserById(userId);
        }

        @Test
        void shouldReturnEmptyPageWhenNoAccountsFound() {
            AccountFiltersDTO filters = new AccountFiltersDTO("Conta Nula", null);
            Pageable pageable = PageRequest.of(0, 10);

            Page<Account> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(accountRespository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            PageResponseDTO<ResponseAccountDTO> result = accountService.list(filters, pageable);

            assertNotNull(result);
            assertTrue(result.content().isEmpty());

            verify(accountRespository, times(1)).findAll(any(Specification.class), eq(pageable));
            verifyNoInteractions(userClientCacheService);
        }
    }

    @Nested
    @DisplayName("Tests of the update method")
    class UpdateMethodTests {

        @Test
        void shouldUpdateAccountSuccessfully() {
            UUID id = UUID.randomUUID();

            Account account = new Account();
            account.setId(id);
            account.setUserId(userId);
            account.setName("Caixa");
            account.setType(TypeEnum.CASH);
            account.setInitialBalance(BigDecimal.ZERO);
            account.setCurrentBalance(BigDecimal.ZERO);

            UpdateAccountDTO dto = new UpdateAccountDTO("Conta Nubank", TypeEnum.INVESTMENT);

            when(accountRespository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(account));
            when(accountRespository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ResponseAccountDTO result = accountService.update(id, dto);

            assertNotNull(result);
            assertEquals("Conta Nubank", result.name());
            assertEquals(TypeEnum.INVESTMENT, result.type());
        }

        @Test
        void shouldThrowExceptionWhenAccountNotFound() {
            UUID nonExistentAccountId = UUID.randomUUID();
            UpdateAccountDTO updateDTO = new UpdateAccountDTO("Conta Inexsistente", TypeEnum.CASH);

            when(accountRespository.findByIdAndUserId(nonExistentAccountId, userId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFound.class, () -> accountService.update(nonExistentAccountId, updateDTO));

            verify(accountRespository, times(1)).findByIdAndUserId(nonExistentAccountId, userId);
            verify(accountMapper, never()).updateAccountFromDTO(any(), any());
            verify(accountRespository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests of the delete method")
    class DeleteMethodTests {

        @Test
        void shouldDeleteAccountSuccessfullyWhenIdExists() {
            UUID accountId = UUID.randomUUID();

            Account account = new Account();
            account.setId(accountId);
            account.setUserId(userId);
            account.setName("Caixa");
            account.setType(TypeEnum.CASH);
            account.setInitialBalance(BigDecimal.ZERO);
            account.setCurrentBalance(BigDecimal.ZERO);

            when(accountRespository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));

            assertDoesNotThrow(() -> accountService.delete(accountId));

            verify(accountRespository, times(1)).findByIdAndUserId(accountId, userId);
            verify(accountRespository, times(1)).delete(account);
        }

        @Test
        void shouldThrowAccountNotFoundWhenIdDoesNotExist() {
            UUID nonExistentId = UUID.randomUUID();

            when(accountRespository.findByIdAndUserId(nonExistentId, userId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFound.class, () -> accountService.delete(nonExistentId));

            verify(accountRespository, times(1)).findByIdAndUserId(nonExistentId, userId);
            verify(accountRespository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Tests of the getInternalAccount method")
    class GetInternalAccountMethodTests {

        @Test
        void shouldReturnAccountSummaryDTOWhenAccountExists() {
            UUID accountId = UUID.randomUUID();

            Account account = new Account();
            account.setId(accountId);
            account.setUserId(userId);
            account.setName("Caixa");
            account.setType(TypeEnum.CASH);
            account.setInitialBalance(BigDecimal.ZERO);
            account.setCurrentBalance(BigDecimal.ZERO);

            when(accountRespository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(account));

            AccountSummaryDTO result = accountService.getInternalAccount(accountId);

            assertEquals(accountId, result.id());
            assertEquals(account.getName(), result.name());

            verify(accountRespository, times(1)).findByIdAndUserId(accountId, userId);
        }

        @Test
        void shouldThrowAccountNotFoundWhenIdDoesNotExist() {
            UUID nonExistentId = UUID.randomUUID();

            when(accountRespository.findByIdAndUserId(nonExistentId, userId)).thenReturn(Optional.empty());

            assertThrows(AccountNotFound.class, () -> accountService.getInternalAccount(nonExistentId));

            verify(accountRespository, times(1)).findByIdAndUserId(nonExistentId, userId);
        }
    }
}