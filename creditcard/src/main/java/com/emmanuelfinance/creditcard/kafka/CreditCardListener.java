package com.emmanuelfinance.creditcard.kafka;

import com.emmanuelfinance.creditcard.services.CreditCardBalanceService;
import com.emmanuelfinance.creditcard.services.CreditCardInternalService;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditCardListener {

    private final CreditCardInternalService creditCardInternalService;
    private final CreditCardBalanceService creditCardBalanceService;
    private final CreditCardProducer creditCardProducer;

    @KafkaListener(topics = "account-events", groupId = "credit-card-service-group")
    public void handleAccountDeleted(AccountEventDTO event) {
        log.info("Recebido evento de conta deletada: {}", event.accountId());

        try {
            creditCardInternalService.deactivateCardsByAccountId(event.accountId());
        } catch (Exception e) {
            log.error("Erro ao deletar cartões associados {}", event, e);
        }
    }

    @KafkaListener(topics = "transaction-created-topic", groupId = "credit-card-service-group")
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Recebido evento de transação criada para o cartão: {}", event.creditCardId());

        if (event.creditCardId() == null) {
            return;
        }

        try {
            creditCardBalanceService.processTransactionCreate(event);
        } catch (Exception e) {
            creditCardProducer.publishTransactionFailed(event.transactionId());
            log.error("Erro ao processar atualização de saldo e limite para o evento de criação: {}", event, e);
        }
    }

    @KafkaListener(topics = "transaction-deleted-topic", groupId = "credit-card-service-group")
    public void handleTransactionDeleted(TransactionDeletedAndRestoreEvent event) {
        log.info(
                "Recebido evento de transação excluída no cartão (Transaction ID: {} Card ID: {})",
                event.transactionId(),
                event.creditCardId()
        );

        try {
            creditCardBalanceService.processTransactionDeletion(event);
        } catch (Exception e) {
            creditCardProducer.publishTransactionFailed(event.transactionId());
            log.error("Erro ao processar exclusão de transação no cartão: {}", event, e);
        }
    }

    @KafkaListener(topics = "transaction-restore-topic", groupId = "credit-card-group")
    public void handleTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        log.info(
                "Recebido evento de transação recuperada no cartão (Transaction ID: {} Card ID: {})",
                event.transactionId(),
                event.creditCardId()
        );

        try {
            creditCardBalanceService.processTransactionRestore(event);
        } catch (Exception e) {
            log.error("Erro ao processar atualização de saldo no cartão para o evento de recuperação: {}", event, e);
        }
    }
}