package com.emmanuelfinance.transaction.transaction.services.scheduler;

import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.transaction.outbox.OutboxService;
import com.emmanuelfinance.transaction.transaction.services.TransactionOutboxEventsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionExecutionService {

    private final TransactionOutboxEventsService transactionOutboxEventsService;
    private final TransactionSelector transactionSelector;
    private final TransactionRepository transactionRepository;
    private final OutboxService outboxService;

    // executa uma transação agendada
    @Transactional
    public void executeScheduledTransaction(UUID transactionId) {
        log.info("Iniciando execução da transação agendadada: {}", transactionId);

        // pega a transação
        Transaction transaction = transactionSelector.getTransactionByIdInternal(transactionId);

        // verifica se está deletada ou o status não é PENDING
        if (transaction.isDeleted() || !StatusTransactionEnum.PENDING.equals(transaction.getStatus())) {
            log.warn("Transação {} ignorada (deleted: {}, status: {})",
                    transactionId, transaction.isDeleted(), transaction.getStatus());
            return;
        }

        // se a data mudou para uma data futura não executa
        if (transaction.getDate().isAfter(LocalDateTime.now())) {
            log.info("Transação {} teve a data alterada para {}. Execução atual abortada.",
                    transactionId, transaction.getDate());
            return;
        }

        try {
            transaction.setStatus(StatusTransactionEnum.PAID);
            // grava na hora para a data de atualização já ir preenchida na foto do outbox
            transactionRepository.saveAndFlush(transaction);

            // publica evento de transação (snapshot)
            outboxService.transactionChanged(transaction);

            // publica evento no kafka
            transactionOutboxEventsService.publishTransactionCreatedEventIfPaid(transaction);
        } catch (Exception e) {
            log.error("Erro ao executar transação agendada {}: {}", transactionId, e.getMessage());
            transaction.setStatus(StatusTransactionEnum.PENDING);
            // volta para pendente também no mongo, senão ele ficaria mostrando a transação como paga
            transactionRepository.saveAndFlush(transaction);
            outboxService.transactionChanged(transaction);
        }
    }
}
