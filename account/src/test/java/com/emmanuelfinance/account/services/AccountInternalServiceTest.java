package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountSelector;
import com.emmanuelfinance.account.AccountTestDataBuilder;
import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountInternalServiceTest {

    @InjectMocks
    private AccountInternalService accountInternalService;

    @Mock
    private AccountSelector accountSelector;

    private Account account;

    @BeforeEach
    void setUp() {
        CreateAccountDTO accountDTO = AccountTestDataBuilder.createAccountDTO();
        account = AccountTestDataBuilder.accountEntity(accountDTO, UUID.randomUUID(), false);
    }

    @Nested()
    @DisplayName("Tests of getAccountSummary method")
    class GetAccountSummaryMethod {

        @Test
        @DisplayName("Deve dar erro quando a conta não for encontrada")
        void shouldGiveErrorWhenAccountIsNotFound() {
            when(accountSelector.getAccountByIdIncludingDeleted(account.getId()))
                    .thenThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            AccountDomainException exception = assertThrows(AccountDomainException.class, () -> {
                accountInternalService.getAccountSummary(account.getId());
            });

            assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Deve retornar uma AccountSummary da conta passada")
        void shouldReturnTheSummaryAccount() {
            when(accountSelector.getAccountByIdIncludingDeleted(account.getId()))
                    .thenReturn(account);

            AccountSummaryDTO accountSummary = accountInternalService.getAccountSummary(account.getId());

            assertEquals(account.getId(), accountSummary.id());

        }
    }

    @Nested()
    @DisplayName("Tests of getAccountSummaryInternal method")
    class GetAccountSummaryInternalMethod {

        @Test
        @DisplayName("Deve dar erro quando a conta não for encontrada")
        void shouldGiveErrorWhenAccountIsNotFound() {
            when(accountSelector.getAccountByIdIncludingDeleted(account.getId()))
                    .thenThrow(new AccountDomainException(AccountErrorCode.ACCOUNT_NOT_FOUND));

            AccountDomainException exception = assertThrows(AccountDomainException.class, () -> {
                accountInternalService.getAccountSummaryInternal(account.getId());
            });

            assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Deve retornar uma AccountSummaryInternal da conta passada")
        void shouldReturnTheSummaryAccount() {
            when(accountSelector.getAccountByIdIncludingDeleted(account.getId()))
                    .thenReturn(account);

            AccountSummaryInternalDTO accountSummary = accountInternalService
                    .getAccountSummaryInternal(account.getId());

            assertEquals(account.getId(), accountSummary.id());
            assertEquals(account.isDeleted(), accountSummary.deleted());
        }
    }
}
