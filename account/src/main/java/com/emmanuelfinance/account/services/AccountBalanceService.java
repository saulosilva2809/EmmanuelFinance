package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountRepository;
import com.emmanuelfinance.account.AccountSelector;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.TransactionUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountBalanceService {

    private final AccountRepository accountRepository;
    private final AccountSelector accountSelector;

    @Transactional
    public void updateBalanceFromTransaction(TransactionCreatedEvent event) {
        Account account = accountSelector.getAccountByIdIncludingDeleted(event.accountId());
        applyBalance(account, event.amount(), event.type(), false);
        accountRepository.save(account);
    }

    @Transactional
    public void updateBalanceFromUpdatedTransaction(TransactionUpdatedEvent event) {
        Account oldAccount = accountSelector.getAccountByIdIncludingDeleted(event.oldAccountId());
        applyBalance(oldAccount, event.oldAmount(), event.oldType(), true);
        accountRepository.save(oldAccount);

        Account newAccount = event.oldAccountId().equals(event.newAccountId())
                ? oldAccount
                : accountSelector.getAccountByIdIncludingDeleted(event.newAccountId());

        applyBalance(newAccount, event.newAmount(), event.newType(), false);
        accountRepository.save(newAccount);
    }

    private void applyBalance(Account account, BigDecimal amount, TypeEnum type, boolean isReversal) {
        boolean isIncome = type == TypeEnum.INCOME;
        boolean shouldAdd = isReversal != isIncome;

        BigDecimal updatedBalance = shouldAdd
                ? account.getCurrentBalance().add(amount)
                : account.getCurrentBalance().subtract(amount);

        account.setCurrentBalance(updatedBalance);
    }
}