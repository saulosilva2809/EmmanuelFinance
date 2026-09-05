package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.account.services.AccountBalanceService;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountListener {

    private final AccountBalanceService accountBalanceService;

    @KafkaListener(topics = "transaction-created-topic", groupId = "account-service-group")
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Recebido evento de transação criada para a conta: {}", event.accountId());

        if (event.creditCardId() != null) {
            return;
        }

        try {
            accountBalanceService.updateBalanceFromTransaction(event);
        } catch (Exception e) {
            log.error("Erro ao processar atualização de saldo para o evento de criação: {}", event, e);
        }
    }

    @KafkaListener(topics = "transaction-updated-topic", groupId = "account-service-group")
    public void handleTransactionUpdated(TransactionUpdatedEvent event) {
        log.info("Recebido evento de transação atualizada (ID: {})", event.transactionId());

        try {
            accountBalanceService.updateBalanceFromUpdatedTransaction(event);
        } catch (Exception e) {
            log.error("Erro ao processar atualização de saldo para o evento de alteração: {}", event, e);
        }
    }

    @KafkaListener(topics = "transaction-deleted-topic", groupId = "account-service-group")
    public void handleTransactionDeleted(TransactionDeletedAndRestoreEvent event) {
        log.info("Recebido evento de transação recuperada (ID: {})", event.transactionId());

        try {
            accountBalanceService.updateBalanceFromDeletedTransaction(event);
        } catch (Exception e) {
            log.error("Erro ao processar atualização de saldo para o evento de alteração: {}", event, e);
        }
    }

    @KafkaListener(topics = "transaction-restore-topic", groupId = "account-service-group")
    public void handleTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        log.info("Recebido evento de transação recuperada (ID: {})", event.transactionId());

        try {
            accountBalanceService.updateBalanceFromRestoreTransaction(event);
        } catch (Exception e) {
            log.error("Erro ao processar atualização de saldo para o evento de alteração: {}", event, e);
        }
    }
}