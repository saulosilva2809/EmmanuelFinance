package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountRepository;
import com.emmanuelfinance.account.AccountSelector;
import com.emmanuelfinance.account.outbox.OutboxService;
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
    private final OutboxService outboxService;

    // atualiza saldo na criação de uma transação
    @Transactional
    public void updateBalanceFromTransaction(TransactionCreatedEvent event) {
        Account account = accountSelector.getAccountByIdInternal(event.accountId());

        // aplica o valor (conta, valor, tipo, se é uma reversão)
        // quando é uma reversão, ele remove um valor e adiciona outro
        applyBalance(account, event.amount(), event.type(), false);

        // save no postgres e mongo
        saveAndRecordChange(account);
    }

    // atualiza o saldo na atualização de uma transação já existente
    @Transactional
    public void updateBalanceFromUpdatedTransaction(TransactionUpdatedEvent event) {

        // pega a conta antiga da transação (caso tenha mudado)
        Account oldAccount = accountSelector.getAccountByIdInternal(event.oldAccountId());

        // se o status da transação antiga está como PAGO
        if (StatusTransactionEnum.PAID.equals(event.oldStatus())) {
            // se ela não está paga (PENDING) não precisa reverter (pois transações PENDING não descontam
            // o saldo da conta, só desconta após ela passar para PAID)

            // aplica o valor na conta, revertendo o antigo
            applyBalance(oldAccount, event.oldAmount(), event.oldType(), true);

            // salva no postgres e mongo
            saveAndRecordChange(oldAccount);
        }

        // verifica se o id que veio em oldAccountId é o mesmo de newAccountId
        // se sim, pega a conta antiga
        // se não
        // busca a conta nova
        Account newAccount = event.oldAccountId().equals(event.newAccountId())
                ? oldAccount
                : accountSelector.getAccountByIdInternal(event.newAccountId());

        // se o status da transação é PAGO
        if (StatusTransactionEnum.PAID.equals(event.newStatus())) {

            // aplica o valor na nova conta
            applyBalance(newAccount, event.newAmount(), event.newType(), false);
            saveAndRecordChange(newAccount);
        }
    }

    // atualiza o saldo da conta após deletar uma transação
    @Transactional
    public void updateBalanceFromDeletedTransaction(TransactionDeletedAndRestoreEvent event) {
        // pega a conta com o id informado
        Account account = accountSelector.getAccountByIdInternal(event.accountId());

        // reverte o valor da transação
        applyBalance(account, event.amount(), event.type(), true);
        saveAndRecordChange(account);
    }

    // atualiza o saldo da conta após restaurar uma transação
    @Transactional
    public void updateBalanceFromRestoreTransaction(TransactionDeletedAndRestoreEvent event) {
        // pega a conta com o id informado
        Account account = accountSelector.getAccountByIdInternal(event.accountId());

        // aplica o valor da transação
        applyBalance(account, event.amount(), event.type(), false);
        saveAndRecordChange(account);
    }

    // salva no Postgres e registra no Outbox, para o Mongo refletir o novo saldo
    private void saveAndRecordChange(Account account) {
        accountRepository.saveAndFlush(account);
        outboxService.accountBalanceChanged(account);
    }

    private void applyBalance(Account account, BigDecimal amount, TypeEnum type, boolean isReversal) {
        // se a conta ou o tipo está nulo encerra
        if (amount == null || type == null) {
            return;
        }

        // variável para detectar se é uma receita
        boolean isIncome = type == TypeEnum.INCOME;

        // variável que define se deve somar ou subtrair
        // condição ? se_verdadeiro : se_falso
        // pega o valor de isReversal
        // se isReversal (é reversão) for verdadeiro:
            // pega o valor de inIncome e inverte
        // se isReversal for falso:
            // pega o valor de isIncome
        boolean shouldAdd = isReversal ? !isIncome : isIncome;

        // se account.getCurrentBalance() não for nulo:
            // pega account.getCurrentBalance()
        // se account.getCurrentBalance() for nulo:
            // pega BigDecimal.ZERO
        BigDecimal currentBalance = account.getCurrentBalance() != null
                ? account.getCurrentBalance()
                : BigDecimal.ZERO;

        // se shouldAdd for verdadeiro:
            // soma o valor na conta
        // se shouldAdd for false:
            // subtrai o valor da conta
        BigDecimal updatedBalance = shouldAdd
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);

        // atribui o novo saldo da conta
        account.setCurrentBalance(updatedBalance);
    }
}