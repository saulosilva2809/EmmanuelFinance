package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.exceptions.AccountNotFound;
import com.emmanuelfinance.account.exceptions.RestoreAccountError;
import com.emmanuelfinance.account.kafka.AccountEventPublisher;
import com.emmanuelfinance.account.services.AccountService;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Mock
    private UserClientCacheService userClientCacheService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AccountEventPublisher accountEventPublisher;

    @Mock
    private AccountCache accountCache;

    @Mock
    private AccountSelector accountSelector;

    @Spy
    private AccountMapper accountMapper = Mappers.getMapper(AccountMapper.class);

    private UUID userId;

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        userId = securityUtils.getCurrentUserId();
    }

    @Nested
    @DisplayName("Tests of the create method")
    class CreateMethodTests {

        @Test
        void shouldCreateAccountSuccessfully() {
            // preparação
            UUID generatedAccountId = UUID.randomUUID();

            UserSummaryDTO mockUser = new UserSummaryDTO(userId, "saulocomercial7@gmail.com");
            when(userClientCacheService.getUserById(userId)).thenReturn(mockUser);

            CreateAccountDTO createDTO = new CreateAccountDTO(
                    "Conta Itaú",
                    TypeEnum.CHECKING,
                    BanksEnum.C6_BANK,
                    new BigDecimal("1000.00")
            );

            // ensinando o repository a simular a geração do ID ao salvar
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account accountToSave = invocation.getArgument(0);
                accountToSave.setId(generatedAccountId);
                return accountToSave;
            });

            // execucão
            ResponseAccountDTO result = accountService.create(createDTO);

            // validação
            assertNotNull(result);
            assertEquals(generatedAccountId, result.id());
            assertEquals("Conta Itaú", result.name());
            assertEquals(new BigDecimal("1000.00"), result.initialBalance());
            assertEquals(new BigDecimal("1000.00"), result.currentBalance());

            // comportamento do mock
            verify(accountRepository, times(1)).save(argThat(
                    account -> account.getUserId().equals(userId) &&
                            account.getName().equals("Conta Itaú") &&
                            account.getInitialBalance().equals(new BigDecimal("1000.00"))
            ));
            verify(userClientCacheService, times(1)).getUserById(userId);
            verify(accountEventPublisher, times(1)).publishAccount(any(AccountEventDTO.class));
        }
    }

    @Nested
    @DisplayName("Tests of the view method")
    class ViewMethodTests {

        @Test
        void shouldThrowAccountNotFoundWhenAccountDoesNotExist() {
            // preparação
            UUID nonExistentAccountId = UUID.randomUUID();

            // ensinando o selector a lançar a exceção
            when(accountSelector.getAccountByIdAndUserId(nonExistentAccountId))
                    .thenThrow(new AccountNotFound());

            // when e then
            // valida a chamada do method e lança a exeção
            assertThrows(AccountNotFound.class, () -> {
                accountService.view(nonExistentAccountId);
            });

            // garante que o selector foi chamado
            verify(accountSelector, times(1)).getAccountByIdAndUserId(nonExistentAccountId);

            // verifica se o userClientCacheService não foi chamado
            verifyNoInteractions(userClientCacheService);
        }

        @Test
        void shouldGiveErrorWhenFetchingADeletedAccount() {
            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    true
            );

            when(accountSelector.getAccountByIdAndUserId(accountEntity.getId()))
                    .thenThrow(new AccountNotFound());

            assertThrows(AccountNotFound.class, () -> {
                accountService.view(accountEntity.getId());
            });

            verify(accountSelector, times(1)).getAccountByIdAndUserId(accountEntity.getId());
        }

        @Test
        void shouldReturnTheAccountSuccessfully() {
            // fase de preparação
            UUID accountId = UUID.randomUUID();

            Account mockAccount = new Account();
            mockAccount.setId(accountId);
            mockAccount.setName("Conta Itaú");
            mockAccount.setUserId(userId);
            mockAccount.setType(TypeEnum.INVESTMENT);
            mockAccount.setInitialBalance(new BigDecimal("1000.00"));
            mockAccount.setCurrentBalance(new BigDecimal("1000.00"));

            UserSummaryDTO mockUser = new UserSummaryDTO(userId, "saulocomercial7@gmail.com");

            // ensinando os mocks
            when(accountSelector.getAccountByIdAndUserId(accountId))
                    .thenReturn(mockAccount);
            when(userClientCacheService.getUserById(userId)).thenReturn(mockUser);

            // when
            ResponseAccountDTO result = accountService.view(accountId);

            // then
            assertNotNull(result);
            assertEquals(accountId, result.id());
            assertEquals("Conta Itaú", result.name());
            assertEquals(new BigDecimal("1000.00"), result.initialBalance());
            assertEquals(new BigDecimal("1000.00"), result.currentBalance());

            // verificaões de chamada
            verify(accountSelector, times(1)).getAccountByIdAndUserId(accountId);
            verify(userClientCacheService, times(1)).getUserById(userId);
        }
    }

    @Nested
    @DisplayName("Tests of the getInternalAccount method")
    class GetInternalAccountMethodTests {

        @Test
        void shouldReturnAccountSummaryDTOWhenAccountExists() {
            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    false
            );

            when(accountSelector.getAccountByIdIncludingDeleted(accountEntity.getId()))
                    .thenReturn(accountEntity);

            AccountSummaryDTO result = accountService.getAccountSummary(accountEntity.getId());

            assertEquals(accountEntity.getId(), result.id());
            assertEquals(accountEntity.getName(), result.name());

            verify(accountSelector, times(1)).getAccountByIdIncludingDeleted(accountEntity.getId());
        }

        @Test
        void shouldThrowAccountNotFoundWhenIdDoesNotExist() {
            UUID nonExistentId = UUID.randomUUID();

            when(accountSelector.getAccountByIdIncludingDeleted(nonExistentId))
                    .thenThrow(new AccountNotFound());

            assertThrows(AccountNotFound.class, () -> {
                accountService.getAccountSummary(nonExistentId);
            });

            verify(accountSelector, times(1)).getAccountByIdIncludingDeleted(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Tests of the list method")
    class ListMethodTests {

        @Test
        void shouldReturnEmptyPageWhenNoAccountsFound() {
            AccountFiltersDTO filters = new AccountFiltersDTO("Conta Nula", null);
            Pageable pageable = PageRequest.of(0, 10);

            Page<Account> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            // when
            PageResponseDTO<ResponseAccountDTO> result = accountService.list(filters, pageable);

            // then
            assertNotNull(result);
            assertTrue(result.content().isEmpty()); // valida que o content está vazio

            // verificações
            verify(accountRepository, times(1)).findAll(any(Specification.class), eq(pageable));
            verifyNoInteractions(userClientCacheService);
        }

        @Test
        void shouldReturnPaginatedAccountsSuccessfully() {
            // preparação
            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    false
            );
            UserSummaryDTO mockUser = AccountTestDataBuilder.createUserMock(accountEntity.getId());

            // dtos de filtro e user
            AccountFiltersDTO filters = new AccountFiltersDTO("C6", TypeEnum.CHECKING);
            Pageable pageable = PageRequest.of(0, 10); // page 0, size 10

            // criando a página
            List<Account> accountList = List.of(accountEntity);
            Page<Account> accountPage = new PageImpl<>(accountList, pageable, accountList.size());

            // ensinando os mocks
            when(accountRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(accountPage);
            when(userClientCacheService.getUserById(userId)).thenReturn(mockUser);

            // when
            PageResponseDTO<ResponseAccountDTO> result = accountService.list(filters, pageable);

            // then
            assertNotNull(result);
            assertNotNull(result.content());
            assertEquals(1, result.content().size()); // garante que veio apenas 1 item
            assertEquals(accountEntity.getId(), result.content().get(0).id());
            assertEquals(accountDTO.name(), result.content().get(0).name());

            // verificações
            verify(accountRepository, times(1)).findAll(any(Specification.class), eq(pageable));
            verify(userClientCacheService, times(1)).getUserById(userId);
        }
    }

    @Nested
    @DisplayName("Tests of the update method")
    class UpdateMethodTests {

        @Test
        void shouldThrowExceptionWhenAccountNotFound() {
            UUID nonExistentAccountId = UUID.randomUUID();
            UpdateAccountDTO updateDTO = new UpdateAccountDTO("Conta Inexsistente", TypeEnum.CASH);

            when(accountSelector.getAccountByIdAndUserId(nonExistentAccountId))
                    .thenThrow(new AccountNotFound());

            assertThrows(AccountNotFound.class, () -> {
                accountService.update(nonExistentAccountId, updateDTO);
            });

            verify(accountSelector, times(1)).getAccountByIdAndUserId(nonExistentAccountId);
            verify(accountMapper, never()).updateAccountFromDTO(any(), any());
            verify(accountRepository, never()).save(any());
        }

        @Test
        void shouldGiveErrorWhenFetchingADeletedAccount() {
            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    true
            );
            UpdateAccountDTO updateDTO = new UpdateAccountDTO("Conta Atualizada", null);

            when(accountSelector.getAccountByIdAndUserId(accountEntity.getId()))
                    .thenThrow(new AccountNotFound());

            assertThrows(AccountNotFound.class, () -> {
                accountService.update(accountEntity.getId(), updateDTO);
            });

            verify(accountSelector, times(1)).getAccountByIdAndUserId(accountEntity.getId());
        }

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

            when(accountSelector.getAccountByIdAndUserId(id))
                    .thenReturn(account);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ResponseAccountDTO result = accountService.update(id, dto);

            assertNotNull(result);
            assertEquals("Conta Nubank", result.name());
            assertEquals(TypeEnum.INVESTMENT, result.type());
        }
    }

    @Nested
    @DisplayName("Tests of the delete method")
    class DeleteMethodTests {

        @Test
        void shouldThrowAccountNotFoundWhenIdDoesNotExist() {
            UUID nonExistentId = UUID.randomUUID();

            when(accountSelector.getAccountByIdAndUserId(nonExistentId))
                    .thenThrow(new AccountNotFound());

            assertThrows(AccountNotFound.class, () -> {
                accountService.delete(nonExistentId);
            });

            verify(accountSelector, times(1)).getAccountByIdAndUserId(nonExistentId);
            verify(accountRepository, never()).deleteById(any());
        }

        @Test
        void shouldGiveErrorWhenFetchingADeletedAccount() {
            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    true
            );

            when(accountSelector.getAccountByIdAndUserId(accountEntity.getId()))
                    .thenThrow(new AccountNotFound());

            assertThrows(AccountNotFound.class, () -> {
                accountService.delete(accountEntity.getId());
            });

            verify(accountSelector, times(1)).getAccountByIdAndUserId(accountEntity.getId());
        }

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

            when(accountSelector.getAccountByIdAndUserId(accountId))
                    .thenReturn(account);

            assertDoesNotThrow(() -> accountService.delete(accountId));

            verify(accountSelector, times(1)).getAccountByIdAndUserId(accountId);
            verify(accountRepository, times(1)).delete(account);
            verify(accountEventPublisher, times(1)).publishAccount(any(AccountEventDTO.class));
        }
    }

    @Nested
    @DisplayName("Tests of restore method")
    class RestoreMethodTests {

        @Test
        void shouldErrorWhenTryingToRestoreANonDeletedAccount() {
            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    false
            );

            when(accountSelector.getAccountByIdIncludingDeleted(accountEntity.getId()))
                    .thenReturn(accountEntity);

            assertThrows(RestoreAccountError.class, () -> {
                accountService.restore(accountEntity.getId());
            });

            verify(accountSelector, times(1)).getAccountByIdIncludingDeleted(accountEntity.getId());
            verify(accountEventPublisher, never()).publishAccount(any(AccountEventDTO.class));
        }

        @Test
        void shouldRestoreAnAccountSuccessfully() {
            CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
            Account accountEntity = AccountTestDataBuilder.accountEntity(
                    accountDTO,
                    userId,
                    true
            );

            when(accountSelector.getAccountByIdIncludingDeleted(accountEntity.getId()))
                    .thenReturn(accountEntity);

            accountService.restore(accountEntity.getId());

            verify(accountSelector, times(1)).getAccountByIdIncludingDeleted(accountEntity.getId());
            verify(accountEventPublisher, times(1)).publishAccount(any(AccountEventDTO.class));
        }
    }
}
