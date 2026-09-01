package com.emmanuelfinance.transaction.services.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

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

    public void schedule(UUID transactionId, LocalDateTime scheduledDateTime) {
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
