package com.emmanuelfinance.transaction.services.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionSchedulerServiceTest {

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private TransactionExecutionService transactionExecutionService;

    @Mock
    private ScheduledFuture<?> future;

    @InjectMocks
    private TransactionSchedulerService transactionSchedulerService;

    @Test
    @DisplayName("Deve agendar a execução no instante da data informada")
    void shouldScheduleAtGivenInstant() {
        UUID transactionId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.now().plusHours(1);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        transactionSchedulerService.schedule(transactionId, date);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), instantCaptor.capture());
        assertEquals(date.atZone(ZoneId.systemDefault()).toInstant(), instantCaptor.getValue());
    }

    @Test
    @DisplayName("Deve executar a transação agendada quando a tarefa rodar")
    void shouldExecuteTransactionWhenTaskRuns() {
        UUID transactionId = UUID.randomUUID();
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        transactionSchedulerService.schedule(transactionId, LocalDateTime.now().plusHours(1));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        runnableCaptor.getValue().run();

        verify(transactionExecutionService, times(1)).executeScheduledTransaction(transactionId);
    }

    @Test
    @DisplayName("Deve cancelar o agendamento anterior ao reagendar a mesma transação")
    void shouldCancelPreviousScheduleWhenRescheduling() {
        UUID transactionId = UUID.randomUUID();
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        transactionSchedulerService.schedule(transactionId, LocalDateTime.now().plusHours(1));
        transactionSchedulerService.schedule(transactionId, LocalDateTime.now().plusHours(2));

        verify(future, times(1)).cancel(false);
        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Deve cancelar o agendamento existente")
    void shouldCancelExistingSchedule() {
        UUID transactionId = UUID.randomUUID();
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        transactionSchedulerService.schedule(transactionId, LocalDateTime.now().plusHours(1));

        transactionSchedulerService.cancel(transactionId);

        verify(future, times(1)).cancel(false);
    }

    @Test
    @DisplayName("Não deve falhar ao cancelar uma transação sem agendamento")
    void shouldNotFailWhenCancellingUnscheduledTransaction() {
        assertDoesNotThrow(() -> transactionSchedulerService.cancel(UUID.randomUUID()));

        verifyNoInteractions(future);
    }
}
