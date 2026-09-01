package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountRepository;
import com.emmanuelfinance.account.AccountSelector;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceService {

    private final AccountRepository accountRepository;
    private final AccountSelector accountSelector;

    @Transactional
    public void updateBalanceFromTransaction(TransactionCreatedEvent event) {
        Account account = accountSelector.getAccountByIdInternal(event.accountId());
        applyBalance(account, event.amount(), event.type(), false);
        accountRepository.saveAndFlush(account);
    }

    @Transactional
    public void updateBalanceFromUpdatedTransaction(TransactionUpdatedEvent event) {
        Account oldAccount = accountSelector.getAccountByIdInternal(event.oldAccountId());

        if (StatusTransactionEnum.PAID.equals(event.oldStatus())) {
            applyBalance(oldAccount, event.oldAmount(), event.oldType(), true);
            accountRepository.saveAndFlush(oldAccount);
        }

        Account newAccount = event.oldAccountId().equals(event.newAccountId())
                ? oldAccount
                : accountSelector.getAccountByIdInternal(event.newAccountId());

        if (StatusTransactionEnum.PAID.equals(event.newStatus())) {
            applyBalance(newAccount, event.newAmount(), event.newType(), false);
            accountRepository.saveAndFlush(newAccount);
        }
    }

    @Transactional
    public void updateBalanceFromDeletedTransaction(TransactionDeletedAndRestoreEvent event) {
        Account account = accountSelector.getAccountByIdInternal(event.accountId());

        applyBalance(account, event.amount(), event.type(), true);
        accountRepository.saveAndFlush(account);
    }

    @Transactional
    public void updateBalanceFromRestoreTransaction(TransactionDeletedAndRestoreEvent event) {
        Account account = accountSelector.getAccountByIdInternal(event.accountId());

        applyBalance(account, event.amount(), event.type(), false);
        accountRepository.saveAndFlush(account);
    }

    private void applyBalance(Account account, BigDecimal amount, TypeEnum type, boolean isReversal) {
        if (amount == null || type == null) {
            return;
        }

        boolean isIncome = type == TypeEnum.INCOME;
        boolean shouldAdd = isReversal ? !isIncome : isIncome;

        BigDecimal currentBalance = account.getCurrentBalance() != null
                ? account.getCurrentBalance()
                : BigDecimal.ZERO;

        BigDecimal updatedBalance = shouldAdd
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);

        account.setCurrentBalance(updatedBalance);
    }
}