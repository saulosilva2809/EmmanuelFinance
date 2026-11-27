package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.account.services.AccountBalanceService;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferDeletedAndRestoreEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferUpdatedEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountListener {

    private final AccountBalanceService accountBalanceService;

    // eventos de transação
    // nenhum método captura o erro: é ele subindo que faz o kafka tentar de novo
    @KafkaListener(topics = "transaction-created-topic", groupId = "account-service-group")
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Recebido evento de transação criada para a conta: {}", event.accountId());

        if (event.creditCardId() != null) {
            return;
        }

        accountBalanceService.updateBalanceFromTransaction(event);
    }

    @KafkaListener(topics = "invoice-payment-created-topic", groupId = "account-service-group")
    public void handleInvoicePaymentCreated(TransactionCreatedEvent event) {
        log.info("Recebido evento de transação (pagamento da fatura) criada para a conta: {}", event.accountId());

        accountBalanceService.updateBalanceFromTransaction(event);
    }

    @KafkaListener(topics = "transaction-updated-topic", groupId = "account-service-group")
    public void handleTransactionUpdated(TransactionUpdatedEvent event) {
        log.info("Recebido evento de transação atualizada (ID: {})", event.transactionId());

        accountBalanceService.updateBalanceFromUpdatedTransaction(event);
    }

    @KafkaListener(topics = "transaction-deleted-topic", groupId = "account-service-group")
    public void handleTransactionDeleted(TransactionDeletedAndRestoreEvent event) {
        log.info("Recebido evento de transação excluida (ID: {})", event.transactionId());

        // compra no cartão nunca mexeu no saldo, então não há o que desfazer
        if (event.creditCardId() != null) {
            return;
        }

        accountBalanceService.updateBalanceFromDeletedTransaction(event);
    }

    @KafkaListener(topics = "transaction-restore-topic", groupId = "account-service-group")
    public void handleTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        log.info("Recebido evento de transação recuperada (ID: {})", event.transactionId());

        if (event.creditCardId() != null) {
            return;
        }

        accountBalanceService.updateBalanceFromRestoreTransaction(event);
    }

    // eventos de transferência
    @KafkaListener(topics = "transfer-created-topic", groupId = "account-service-group")
    public void handleTransferCreated(TransferCreatedEventDTO event) {
        log.info(
                "Recebido evento de transferência criada. Conta original: {} -> Conta destino: {}",
                event.sourceAccountId(), event.destinationAccountId()
        );

        accountBalanceService.updateBalanceFromTransfer(event);
    }

    @KafkaListener(topics = "transfer-updated-topic", groupId = "account-service-group")
    public void handleTransferUpdated(TransferUpdatedEventDTO event) {
        log.info("Recebido evento de transferência atualizada (ID: {})", event.transferId());

        accountBalanceService.updateBalanceFromUpdatedTransfer(event);
    }

    @KafkaListener(topics = "transfer-deleted-topic", groupId = "account-service-group")
    public void handleTransferDeleted(TransferDeletedAndRestoreEventDTO event) {
        log.info("Recebido evento de transferência excluída (ID: {})", event.transferId());

        accountBalanceService.updateBalanceFromDeletedTransfer(event);
    }

    @KafkaListener(topics = "transfer-restore-topic", groupId = "account-service-group")
    public void handleTransferRestore(TransferDeletedAndRestoreEventDTO event) {
        log.info("Recebido evento de transferência recuperada (ID: {})", event.transferId());

        accountBalanceService.updateBalanceFromRestoreTransfer(event);
    }
}
