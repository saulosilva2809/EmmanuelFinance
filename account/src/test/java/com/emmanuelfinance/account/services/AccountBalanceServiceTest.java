package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountRepository;
import com.emmanuelfinance.account.AccountSelector;
import com.emmanuelfinance.account.AccountTestDataBuilder;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class AccountBalanceServiceTest {

    @InjectMocks
    private AccountBalanceService accountBalanceService;

    @Mock
    private AccountSelector accountSelector;

    @Mock
    private AccountRepository accountRepository;

    private Account account;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        account = AccountTestDataBuilder.accountEntity(AccountTestDataBuilder.createAccountDTO(), userId, false);
    }

    private TransactionCreatedEvent createdEvent(
            UUID accountId,
            BigDecimal amount,
            TypeEnum type
    ) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(),
                accountId,
                null,
                userId,
                amount,
                1,
                type,
                StatusTransactionEnum.PAID,
                LocalDateTime.now()
        );
    }

    private TransactionUpdatedEvent updatedEvent(
            UUID oldAccountId,
            UUID newAccountId,
            BigDecimal oldAmount,
            BigDecimal newAmount,
            TypeEnum oldType,
            TypeEnum newType
    ) {
        return new TransactionUpdatedEvent(
                UUID.randomUUID(),
                oldAccountId,
                newAccountId,
                userId,
                oldAmount,
                newAmount,
                oldType,
                newType,
                StatusTransactionEnum.PAID,
                StatusTransactionEnum.PAID
        );
    }

    private TransactionDeletedAndRestoreEvent deletedAndRestoreEvent(
            UUID accountId,
            BigDecimal amount,
            TypeEnum type
    ) {
        return new TransactionDeletedAndRestoreEvent(
                UUID.randomUUID(),
                accountId,
                null,
                userId,
                amount,
                1,
                type,
                StatusTransactionEnum.PAID,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Tests of updateBalanceFromTransaction method")
    class UpdateBalanceFromTransaction {

        @Test
        @DisplayName("Deve adicionar no saldo da conta")
        void shouldAddToAccountBalance() {
            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionCreatedEvent event = createdEvent(
                    account.getId(),
                    new BigDecimal(1000),
                    TypeEnum.INCOME
            );

            accountBalanceService.updateBalanceFromTransaction(event);
            assertEquals(new BigDecimal(6000), account.getCurrentBalance());
            log.info("Saldo esperado: 6000 - Saldo presente na conta: {}", account.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve retirar no saldo da conta")
        void shouldSubtractFromAccountBalance() {
            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionCreatedEvent event = createdEvent(
                    account.getId(),
                    new BigDecimal(1000),
                    TypeEnum.EXPENSE
            );

            accountBalanceService.updateBalanceFromTransaction(event);
            assertEquals(new BigDecimal(4000), account.getCurrentBalance());
            log.info("Saldo esperado: 4000 - Saldo presente na conta: {}", account.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
        }
    }

    @Nested
    @DisplayName("Tests of updateBalanceFromUpdatedTransaction method")
    class UpdateBalanceFromUpdatedTransaction {

        @Test
        @DisplayName("Deve alterar a conta da transação quando for receita")
        void shouldChangeTheTransactionAccountWhenItsIncome() {
            // simulando uma transação
            account.setCurrentBalance(account.getCurrentBalance().add(new BigDecimal(1000)));

            Account newAccount = AccountTestDataBuilder.accountEntity(
                    AccountTestDataBuilder.createAccountDTO(),
                    userId,
                    false
            );

            when(accountSelector.getAccountByIdInternal(account.getId())).
                    thenReturn(account);

            when(accountSelector.getAccountByIdInternal(newAccount.getId())).
                    thenReturn(newAccount);

            TransactionUpdatedEvent event = updatedEvent(
                    account.getId(),
                    newAccount.getId(),
                    new BigDecimal(1000),
                    new BigDecimal(1000),
                    TypeEnum.INCOME,
                    TypeEnum.INCOME
            );

            accountBalanceService.updateBalanceFromUpdatedTransaction(event);

            log.info(
                    "Saldo esparado na conta antiga: 5000. Saldo real na conta antiga: {}",
                    account.getCurrentBalance()
            );
            log.info(
                    "Saldo esparado na conta nova: 6000. Saldo real na conta nova: {}",
                    newAccount.getCurrentBalance()
            );

            assertEquals(new BigDecimal(5000), account.getCurrentBalance());
            assertEquals(new BigDecimal(6000), newAccount.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve alterar a conta da transação quando for despesa")
        void shouldChangeTheTransactionAccountWhenItsExpense() {
            // simulando uma transação
            account.setCurrentBalance(account.getCurrentBalance().subtract(new BigDecimal(1000)));

            Account newAccount = AccountTestDataBuilder.accountEntity(
                    AccountTestDataBuilder.createAccountDTO(),
                    userId,
                    false
            );

            when(accountSelector.getAccountByIdInternal(account.getId())).
                    thenReturn(account);

            when(accountSelector.getAccountByIdInternal(newAccount.getId())).
                    thenReturn(newAccount);

            TransactionUpdatedEvent event = updatedEvent(
                    account.getId(),
                    newAccount.getId(),
                    new BigDecimal(1000),
                    new BigDecimal(1000),
                    TypeEnum.EXPENSE,
                    TypeEnum.EXPENSE
            );

            accountBalanceService.updateBalanceFromUpdatedTransaction(event);

            log.info(
                    "Saldo esparado na conta antiga: 5000. Saldo real na conta antiga: {}",
                    account.getCurrentBalance()
            );
            log.info(
                    "Saldo esparado na conta nova: 4000. Saldo real na conta nova: {}",
                    newAccount.getCurrentBalance()
            );

            assertEquals(new BigDecimal(5000), account.getCurrentBalance());
            assertEquals(new BigDecimal(4000), newAccount.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve alterar o saldo da conta quando for receita")
        void shouldChangeTheBalanceInTheAccountWhenItIsIncome() {
            // simulando uma transação
            account.setCurrentBalance(account.getCurrentBalance().add(new BigDecimal(1000)));

            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionUpdatedEvent event = updatedEvent(
                    account.getId(),
                    account.getId(),
                    new BigDecimal(1000),
                    new BigDecimal(2000),
                    TypeEnum.INCOME,
                    TypeEnum.INCOME
            );

            accountBalanceService.updateBalanceFromUpdatedTransaction(event);

            log.info(
                    "Saldo esparado na conta: 7000. Saldo real na conta antiga: {}",
                    account.getCurrentBalance()
            );

            assertEquals(new BigDecimal(7000), account.getCurrentBalance());

            verify(accountRepository, times(2)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve alterar o saldo da conta quando for despesa")
        void shouldChangeTheBalanceInTheAccountWhenItIsAnExpense() {
            // simulando uma transação
            account.setCurrentBalance(account.getCurrentBalance().subtract(new BigDecimal(1000)));

            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionUpdatedEvent event = updatedEvent(
                    account.getId(),
                    account.getId(),
                    new BigDecimal(1000),
                    new BigDecimal(2000),
                    TypeEnum.EXPENSE,
                    TypeEnum.EXPENSE
            );

            accountBalanceService.updateBalanceFromUpdatedTransaction(event);

            log.info(
                    "Saldo esparado na conta: 3000. Saldo real na conta antiga: {}",
                    account.getCurrentBalance()
            );

            assertEquals(new BigDecimal(3000), account.getCurrentBalance());

            verify(accountRepository, times(2)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve alterar o saldo da conta quando altera o tipo de receita para despesa")
        void shouldChangeAccountBalanceWhenIncomeBecomesExpense() {
            // simulando uma transação
            account.setCurrentBalance(account.getCurrentBalance().add(new BigDecimal(1000)));

            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionUpdatedEvent event = updatedEvent(
                    account.getId(),
                    account.getId(),
                    new BigDecimal(1000),
                    new BigDecimal(1000),
                    TypeEnum.INCOME,
                    TypeEnum.EXPENSE
            );

            accountBalanceService.updateBalanceFromUpdatedTransaction(event);

            log.info(
                    "Saldo esparado na conta: 4000. Saldo real na conta antiga: {}",
                    account.getCurrentBalance()
            );

            assertEquals(new BigDecimal(4000), account.getCurrentBalance());

            verify(accountRepository, times(2)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve alterar o saldo da conta quando altera o tipo de despesa para receita")
        void shouldChangeTheAccountBalanceWhenAnExpenseBecomesIncome() {
            // simulando uma transação
            account.setCurrentBalance(account.getCurrentBalance().subtract(new BigDecimal(1000)));

            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionUpdatedEvent event = updatedEvent(
                    account.getId(),
                    account.getId(),
                    new BigDecimal(1000),
                    new BigDecimal(1000),
                    TypeEnum.EXPENSE,
                    TypeEnum.INCOME
            );

            accountBalanceService.updateBalanceFromUpdatedTransaction(event);

            log.info(
                    "Saldo esparado na conta: 6000. Saldo real na conta antiga: {}",
                    account.getCurrentBalance()
            );

            assertEquals(new BigDecimal(6000), account.getCurrentBalance());

            verify(accountRepository, times(2)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve alterar a conta e o saldo após mudar os dados da transação")
        void shouldChangeTheAccountAndBalanceWhenTransactionDataChanges() {
            account.setCurrentBalance(account.getCurrentBalance().add(new BigDecimal(1000)));

            Account newAccount = AccountTestDataBuilder.accountEntity(
                    AccountTestDataBuilder.createAccountDTO(),
                    userId,
                    false
            );

            when(accountSelector.getAccountByIdInternal(account.getId())).
                    thenReturn(account);

            when(accountSelector.getAccountByIdInternal(newAccount.getId())).
                    thenReturn(newAccount);

            TransactionUpdatedEvent event = updatedEvent(
                    account.getId(),
                    newAccount.getId(),
                    new BigDecimal(1000),
                    new BigDecimal(1500),
                    TypeEnum.INCOME,
                    TypeEnum.EXPENSE
            );

            accountBalanceService.updateBalanceFromUpdatedTransaction(event);

            log.info(
                    "Saldo esperado na conta antiga: 5000. Saldo atual na conta antiga: {}",
                    account.getCurrentBalance()
            );

            log.info(
                    "Saldo esperado na conta nova: 3500. Saldo atual na conta antiga: {}",
                    newAccount.getCurrentBalance()
            );

            assertEquals(new BigDecimal(5000), account.getCurrentBalance());
            assertEquals(new BigDecimal(3500), newAccount.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
            verify(accountRepository, times(1)).saveAndFlush(newAccount);
        }
    }

    @Nested
    @DisplayName("Tests of updateBalanceFromDeletedTransaction method")
    class UpdateBalanceFromDeletedTransaction {

        @Test
        @DisplayName("Deve diminuir o saldo da conta quando deletar uma transação do tipo receita")
        void shouldDecreaseAccountBalanceWhenDeletingAnIncomeTransaction() {
            // simulando transação de 1000
            account.setCurrentBalance(account.getCurrentBalance().add(new BigDecimal(1000)));

            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionDeletedAndRestoreEvent event = deletedAndRestoreEvent(
                    account.getId(),
                    new BigDecimal(1000),
                    TypeEnum.INCOME
            );

            accountBalanceService.updateBalanceFromDeletedTransaction(event);
            log.info("Saldo esperado na conta: 5000. Saldo atual na conta: {}", account.getCurrentBalance());

            assertEquals(new BigDecimal(5000), account.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve aumentar o saldo da conta quando deletar uma transação do tipo despesa")
        void shouldIncreaseAccountBalanceWhenDeletingAnIncomeTransaction() {
            // simulando transação de 1000
            account.setCurrentBalance(account.getCurrentBalance().subtract(new BigDecimal(1000)));

            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionDeletedAndRestoreEvent event = deletedAndRestoreEvent(
                    account.getId(),
                    new BigDecimal(1000),
                    TypeEnum.EXPENSE
            );

            accountBalanceService.updateBalanceFromDeletedTransaction(event);
            log.info("Saldo esperado na conta: 5000. Saldo atual na conta: {}", account.getCurrentBalance());

            assertEquals(new BigDecimal(5000), account.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
        }
    }

    @Nested
    @DisplayName("Tests of updateBalanceFromRestoreTransaction method")
    class UpdateBalanceFromRestoreTransaction {

        @Test
        @DisplayName("Deve aumentar o saldo da conta quando restaurar uma transação do tipo receita")
        void shouldIncreaseAccountBalanceWhenRestoringAnIncomeTransaction() {
            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionDeletedAndRestoreEvent event = deletedAndRestoreEvent(
                    account.getId(),
                    new BigDecimal(1000),
                    TypeEnum.INCOME
            );

            accountBalanceService.updateBalanceFromRestoreTransaction(event);
            log.info("Saldo esperado na conta: 6000. Saldo atual na conta: {}", account.getCurrentBalance());

            assertEquals(new BigDecimal(6000), account.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
        }

        @Test
        @DisplayName("Deve diminuir o saldo da conta quando restaurar uma transação do tipo despesa")
        void shouldIncreaseAccountBalanceWhenRestoringAnExpenseTransaction() {
            when(accountSelector.getAccountByIdInternal(account.getId()))
                    .thenReturn(account);

            TransactionDeletedAndRestoreEvent event = deletedAndRestoreEvent(
                    account.getId(),
                    new BigDecimal(1000),
                    TypeEnum.EXPENSE
            );

            accountBalanceService.updateBalanceFromRestoreTransaction(event);
            log.info("Saldo esperado na conta: 4000. Saldo atual na conta: {}", account.getCurrentBalance());

            assertEquals(new BigDecimal(4000), account.getCurrentBalance());

            verify(accountRepository, times(1)).saveAndFlush(account);
        }
    }
}
