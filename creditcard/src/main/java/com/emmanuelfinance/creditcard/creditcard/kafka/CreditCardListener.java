package com.emmanuelfinance.creditcard.creditcard.kafka;

import com.emmanuelfinance.creditcard.creditcard.services.CreditCardBalanceKafkaService;
import com.emmanuelfinance.creditcard.creditcard.services.CreditCardInternalService;
import com.emmanuelfinance.shared.modules.account.dto.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.enums.StatusEventEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditCardListener {

    private final CreditCardInternalService creditCardInternalService;
    private final CreditCardBalanceKafkaService creditCardBalanceKafkaService;

    // nenhum método captura o erro: é ele subindo que faz o kafka tentar de novo
    @KafkaListener(topics = "account-events", groupId = "credit-card-service-group")
    public void handleAccountEvent(AccountEventDTO event) {
        log.info("Recebido evento de conta: {}", event.accountId());

        if (event.status() == StatusEventEnum.DELETED) {
            creditCardInternalService.deactivateCardsByAccountId(event.accountId());
        } else if (event.status() == StatusEventEnum.RESTORE) {
            creditCardInternalService.restoreCardsByAccountId(event.accountId());
        }
    }

    @KafkaListener(topics = "transaction-created-topic", groupId = "credit-card-service-group")
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Recebido evento de transação criada para o cartão: {}", event.creditCardId());

        if (event.creditCardId() == null) {
            return;
        }

        creditCardBalanceKafkaService.processTransactionCreate(event);
    }

    @KafkaListener(topics = "transaction-deleted-topic", groupId = "credit-card-service-group")
    public void handleTransactionDeleted(TransactionDeletedAndRestoreEvent event) {
        log.info(
                "Recebido evento de transação excluída no cartão (Transaction ID: {} Card ID: {})",
                event.transactionId(),
                event.creditCardId()
        );

        // transação só de conta não mexe em cartão nenhum
        if (event.creditCardId() == null) {
            return;
        }

        creditCardBalanceKafkaService.processTransactionDeletion(event);
    }

    @KafkaListener(topics = "transaction-restore-topic", groupId = "credit-card-service-group")
    public void handleTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        log.info(
                "Recebido evento de transação recuperada no cartão (Transaction ID: {} Card ID: {})",
                event.transactionId(),
                event.creditCardId()
        );

        // transação só de conta não mexe em cartão nenhum
        if (event.creditCardId() == null) {
            return;
        }

        creditCardBalanceKafkaService.processTransactionRestore(event);
    }
}
