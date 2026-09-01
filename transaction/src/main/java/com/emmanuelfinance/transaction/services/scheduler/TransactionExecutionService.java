package com.emmanuelfinance.transaction.services.scheduler;

import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.services.TransactionEventsService;
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

    private final TransactionEventsService transactionEventsService;
    private final TransactionSelector transactionSelector;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void executeScheduledTransaction(UUID transactionId) {
        log.info("Iniciando execução da transação agendadada: {}", transactionId);

        Transaction transaction = transactionSelector.getTransactionByIdInternal(transactionId);

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
            transactionRepository.save(transaction);

            transactionEventsService.publishTransactionCreatedEvent(transaction);
        } catch (Exception e) {
            log.error("Erro ao executar transação agendada {}: {}", transactionId, e.getMessage());
            transaction.setStatus(StatusTransactionEnum.PENDING);
            transactionRepository.save(transaction);
        }
    }
}
