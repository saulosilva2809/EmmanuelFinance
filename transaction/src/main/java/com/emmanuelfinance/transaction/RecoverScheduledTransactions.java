package com.emmanuelfinance.transaction;

import com.emmanuelfinance.transaction.services.scheduler.TransactionExecutionService;
import com.emmanuelfinance.transaction.services.scheduler.TransactionSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecoverScheduledTransactions {

    private final TransactionSchedulerService transactionSchedulerService;
    private final TransactionExecutionService transactionExecutionService;
    private final TransactionSelector transactionSelector;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingTransactions() {
        LocalDateTime now = LocalDateTime.now();

        List<Transaction> futureTransactions = transactionSelector.getPendingTransactionsAfter(now);

        for (Transaction transaction : futureTransactions) {
            transactionSchedulerService.schedule(transaction.getId(), transaction.getDate());
        }

        List<Transaction> overdueTransactions = transactionSelector.getPendingTransactionsBefore(now);
        for (Transaction transaction : overdueTransactions) {
            transactionExecutionService.executeScheduledTransaction(transaction.getId());
        }
    }
}