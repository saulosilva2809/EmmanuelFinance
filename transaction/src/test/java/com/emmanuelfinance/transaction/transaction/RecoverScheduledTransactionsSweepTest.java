package com.emmanuelfinance.transaction.transaction;

import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.transaction.services.scheduler.TransactionExecutionService;
import com.emmanuelfinance.transaction.transaction.services.scheduler.TransactionSchedulerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoverScheduledTransactionsSweepTest {

    @Mock
    private TransactionSchedulerService transactionSchedulerService;

    @Mock
    private TransactionExecutionService transactionExecutionService;

    @Mock
    private TransactionSelector transactionSelector;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @InjectMocks
    private RecoverScheduledTransactions recoverScheduledTransactions;

    @Test
    void shouldKeepSweepingTheOverdueTransactions() {
        recoverScheduledTransactions.recoverPendingTransactions();

        verify(taskScheduler).scheduleWithFixedDelay(
                any(Runnable.class), any(Instant.class), eq(Duration.ofMinutes(1)));
    }

    @Test
    void shouldExecuteTheOthersWhenOneFails() {
        Transaction first = TransactionTestDataBuilder.pendingTransactionEntity(UUID.randomUUID(), LocalDateTime.now());
        Transaction second = TransactionTestDataBuilder.pendingTransactionEntity(UUID.randomUUID(), LocalDateTime.now());
        when(transactionSelector.getPendingTransactionsBefore(any())).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("falha"))
                .when(transactionExecutionService).executeScheduledTransaction(first.getId());

        recoverScheduledTransactions.executeOverdueTransactions();

        verify(transactionExecutionService).executeScheduledTransaction(second.getId());
    }
}
