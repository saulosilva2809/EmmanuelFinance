package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
public class AccountSelectorTest {

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private SecurityUtils securityUtils;

    private AccountSelector accountSelector;

    private UUID userId;
    private Account account;
    private Account accountDeleted;

    @BeforeEach
    void setUp() {
        accountSelector = new AccountSelector(accountRepository, securityUtils);

        userId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();

        account = accountRepository.save(AccountTestDataBuilder.accountEntity(accountDTO, userId, false));
        accountDeleted = accountRepository.save(AccountTestDataBuilder.accountEntity(accountDTO, userId, true));
    }

    @Nested
    @DisplayName("Tests of getAccountByIdAndUserId method")
    class GetAccountByIdAndUserIdMethod {

        @Test
        @DisplayName("Deve dar erro quando a conta não for encontrada")
        void shouldGiveErrorWhenAccountIsNotFound() {
            AccountDomainException exception = assertThrows(AccountDomainException.class, () -> {
                accountSelector.getAccountByIdAndUserId(UUID.randomUUID());
            });

            assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Deve dar erro quando a conta for encontrada mas não for do usuário")
        void shouldReturnErrorWhenAccountIsFoundButDoesNotBelongToUser() {
            UUID otherUserId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(otherUserId);

            AccountDomainException exception = assertThrows(AccountDomainException.class, () -> {
                accountSelector.getAccountByIdAndUserId(account.getId());
            });

            assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Deve dar erro ao tentar buscar uma conta deletada")
        void shouldFailWhenTryingToFetchADeletedAccount() {
            AccountDomainException exception = assertThrows(AccountDomainException.class, () -> {
                accountSelector.getAccountByIdAndUserId(accountDeleted.getId());
            });

            assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Deve retornar a conta com sucesso")
        void shouldReturnTheAccountSuccessfully() {
            Account response = accountSelector.getAccountByIdAndUserId(account.getId());

            assertEquals(account.getId(), response.getId());
            assertEquals(account.getName(), response.getName());
        }
    }

    @Nested
    @DisplayName("Tests of getAccountByIdIncludingDeleted method")
    class GetAccountByIdIncludingDeletedMethod {

        @Test
        @DisplayName("Deve dar erro quando a conta não for encontrada")
        void shouldGiveErrorWhenAccountIsNotFound() {
            AccountDomainException exception = assertThrows(AccountDomainException.class, () -> {
                accountSelector.getAccountByIdIncludingDeleted(UUID.randomUUID());
            });

            assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Deve dar erro quando a conta for encontrada mas não for do usuário")
        void shouldReturnErrorWhenAccountIsFoundButDoesNotBelongToUser() {
            UUID otherUserId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(otherUserId);

            AccountDomainException exception = assertThrows(AccountDomainException.class, () -> {
                accountSelector.getAccountByIdIncludingDeleted(account.getId());
            });

            assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Deve retornar a conta deletada com sucesso")
        void shouldReturnTheDeletedAccountSuccessfully() {
            Account response = accountSelector.getAccountByIdIncludingDeleted(accountDeleted.getId());

            assertEquals(accountDeleted.getId(), response.getId());
            assertEquals(accountDeleted.getName(), response.getName());
        }

        @Test
        @DisplayName("Deve retornar a conta com sucesso")
        void shouldReturnTheAccountSuccessfully() {
            Account response = accountSelector.getAccountByIdIncludingDeleted(account.getId());

            assertEquals(account.getId(), response.getId());
            assertEquals(account.getName(), response.getName());
        }
    }
}
