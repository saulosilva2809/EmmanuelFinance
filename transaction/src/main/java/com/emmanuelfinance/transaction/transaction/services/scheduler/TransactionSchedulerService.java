package com.emmanuelfinance.transaction.transaction.services.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSchedulerService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final TransactionExecutionService transactionExecutionService;

    // guarda a referencia de cada agendamento ativo em memória
    private final Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void cancel(UUID transactionId) {
        ScheduledFuture<?> future = scheduledTasks.remove(transactionId);
        if (future != null) {
            future.cancel(false);
            log.info("Agendamento anterior da transação {} foi cancelado.", transactionId);
        }
    }

    // dentro de uma transação do banco, só agenda depois do commit, para a tarefa já encontrar a linha gravada
    public void schedule(UUID transactionId, LocalDateTime scheduledDateTime) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            scheduleNow(transactionId, scheduledDateTime);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                scheduleNow(transactionId, scheduledDateTime);
            }
        });
    }

    private void scheduleNow(UUID transactionId, LocalDateTime scheduledDateTime) {
        // apaga algum agendamento antigo (se houver)
        cancel(transactionId);
        Instant executionTime = scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant();

        log.info("Agendando transação {} para a data: {}", transactionId, scheduledDateTime);

        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> {
                    try {
                        transactionExecutionService.executeScheduledTransaction(transactionId);
                    } finally {
                        scheduledTasks.remove(transactionId);
                    }
                },
                executionTime
        );

        scheduledTasks.put(transactionId, future);
    }
}
