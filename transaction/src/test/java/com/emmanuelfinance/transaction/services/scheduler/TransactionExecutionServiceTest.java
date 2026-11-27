package com.emmanuelfinance.transaction.services.scheduler;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.transaction.outbox.TransactionOutbox;
import com.emmanuelfinance.transaction.transaction.services.TransactionOutboxEventsService;
import com.emmanuelfinance.transaction.transaction.services.scheduler.TransactionExecutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionExecutionServiceTest {

    @Mock
    private TransactionOutboxEventsService transactionOutboxEventsService;

    @Mock
    private TransactionSelector transactionSelector;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionOutbox transactionOutbox;

    @InjectMocks
    private TransactionExecutionService transactionExecutionService;

    private Transaction pending(LocalDateTime date) {
        return TransactionTestDataBuilder.pendingTransactionEntity(UUID.randomUUID(), date);
    }

    @Test
    @DisplayName("Deve marcar como PAID, salvar e publicar o evento quando a data já chegou")
    void shouldExecutePendingTransaction() {
        Transaction transaction = pending(LocalDateTime.now().minusMinutes(1));
        when(transactionSelector.getTransactionByIdForUpdate(transaction.getId())).thenReturn(transaction);

        transactionExecutionService.executeScheduledTransaction(transaction.getId());

        assertEquals(StatusTransactionEnum.PAID, transaction.getStatus());
        verify(transactionRepository, times(1)).saveAndFlush(transaction);
        verify(transactionOutbox, times(1)).transactionChanged(transaction);
        verify(transactionOutboxEventsService, times(1)).publishTransactionCreatedEventIfPaid(transaction);
    }

    @Test
    @DisplayName("Deve ignorar a transação excluída")
    void shouldIgnoreDeletedTransaction() {
        Transaction transaction = pending(LocalDateTime.now().minusMinutes(1));
        transaction.setDeleted(true);
        when(transactionSelector.getTransactionByIdForUpdate(transaction.getId())).thenReturn(transaction);

        transactionExecutionService.executeScheduledTransaction(transaction.getId());

        assertEquals(StatusTransactionEnum.PENDING, transaction.getStatus());
        verifyNoInteractions(transactionRepository, transactionOutboxEventsService, transactionOutbox);
    }

    @Test
    @DisplayName("Deve ignorar a transação que não esteja PENDING")
    void shouldIgnoreNonPendingTransaction() {
        Transaction transaction = pending(LocalDateTime.now().minusMinutes(1));
        transaction.setStatus(StatusTransactionEnum.PAID);
        when(transactionSelector.getTransactionByIdForUpdate(transaction.getId())).thenReturn(transaction);

        transactionExecutionService.executeScheduledTransaction(transaction.getId());

        verifyNoInteractions(transactionRepository, transactionOutboxEventsService, transactionOutbox);
    }

    @Test
    @DisplayName("Não deve executar quando a data foi alterada para o futuro")
    void shouldNotExecuteWhenDateIsInTheFuture() {
        Transaction transaction = pending(LocalDateTime.now().plusDays(1));
        when(transactionSelector.getTransactionByIdForUpdate(transaction.getId())).thenReturn(transaction);

        transactionExecutionService.executeScheduledTransaction(transaction.getId());

        assertEquals(StatusTransactionEnum.PENDING, transaction.getStatus());
        verifyNoInteractions(transactionRepository, transactionOutboxEventsService, transactionOutbox);
    }

    @Test
    @DisplayName("Deve deixar o erro subir, para a transação do banco desfazer tudo")
    void shouldPropagateWhenRegisteringTheEventFails() {
        Transaction transaction = pending(LocalDateTime.now().minusMinutes(1));
        when(transactionSelector.getTransactionByIdForUpdate(transaction.getId())).thenReturn(transaction);
        doThrow(new RuntimeException("banco indisponível"))
                .when(transactionOutboxEventsService).publishTransactionCreatedEventIfPaid(transaction);

        assertThrows(RuntimeException.class,
                () -> transactionExecutionService.executeScheduledTransaction(transaction.getId()));
    }
}
