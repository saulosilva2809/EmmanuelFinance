package com.emmanuelfinance.transaction.transaction;

import com.emmanuelfinance.transaction.transaction.services.scheduler.TransactionExecutionService;
import com.emmanuelfinance.transaction.transaction.services.scheduler.TransactionSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecoverScheduledTransactions {

    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

    private final TransactionSchedulerService transactionSchedulerService;
    private final TransactionExecutionService transactionExecutionService;
    private final TransactionSelector transactionSelector;
    private final ThreadPoolTaskScheduler taskScheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingTransactions() {
        LocalDateTime now = LocalDateTime.now();

        List<Transaction> futureTransactions = transactionSelector.getPendingTransactionsAfter(now);

        for (Transaction transaction : futureTransactions) {
            transactionSchedulerService.schedule(transaction.getId(), transaction.getDate());
        }

        executeOverdueTransactions();

        // a varredura pega as que passaram da data e não foram executadas, por erro ou serviço parado
        taskScheduler.scheduleWithFixedDelay(
                this::executeOverdueTransactions, Instant.now().plus(SWEEP_INTERVAL), SWEEP_INTERVAL);
    }

    public void executeOverdueTransactions() {
        for (Transaction transaction : transactionSelector.getPendingTransactionsBefore(LocalDateTime.now())) {
            try {
                transactionExecutionService.executeScheduledTransaction(transaction.getId());
            } catch (Exception e) {
                // uma que falha não pode segurar as outras, e a próxima varredura tenta de novo
                log.error("Erro ao executar a transação agendada {}", transaction.getId(), e);
            }
        }
    }
}
