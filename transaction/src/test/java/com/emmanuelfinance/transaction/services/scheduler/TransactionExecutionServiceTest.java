package com.emmanuelfinance.transaction.services.scheduler;

import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.services.TransactionEventsService;
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
    private TransactionEventsService transactionEventsService;

    @Mock
    private TransactionSelector transactionSelector;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionExecutionService transactionExecutionService;

    private Transaction pending(LocalDateTime date) {
        return TransactionTestDataBuilder.pendingTransactionEntity(UUID.randomUUID(), date);
    }

    @Test
    @DisplayName("Deve marcar como PAID, salvar e publicar o evento quando a data já chegou")
    void shouldExecutePendingTransaction() {
        Transaction transaction = pending(LocalDateTime.now().minusMinutes(1));
        when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);

        transactionExecutionService.executeScheduledTransaction(transaction.getId());

        assertEquals(StatusTransactionEnum.PAID, transaction.getStatus());
        verify(transactionRepository, times(1)).save(transaction);
        verify(transactionEventsService, times(1)).publishTransactionCreatedEvent(transaction);
    }

    @Test
    @DisplayName("Deve ignorar a transação excluída")
    void shouldIgnoreDeletedTransaction() {
        Transaction transaction = pending(LocalDateTime.now().minusMinutes(1));
        transaction.setDeleted(true);
        when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);

        transactionExecutionService.executeScheduledTransaction(transaction.getId());

        assertEquals(StatusTransactionEnum.PENDING, transaction.getStatus());
        verifyNoInteractions(transactionRepository, transactionEventsService);
    }

    @Test
    @DisplayName("Deve ignorar a transação que não esteja PENDING")
    void shouldIgnoreNonPendingTransaction() {
        Transaction transaction = pending(LocalDateTime.now().minusMinutes(1));
        transaction.setStatus(StatusTransactionEnum.PAID);
        when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);

        transactionExecutionService.executeScheduledTransaction(transaction.getId());

        verifyNoInteractions(transactionRepository, transactionEventsService);
    }

    @Test
    @DisplayName("Não deve executar quando a data foi alterada para o futuro")
    void shouldNotExecuteWhenDateIsInTheFuture() {
        Transaction transaction = pending(LocalDateTime.now().plusDays(1));
        when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);

        transactionExecutionService.executeScheduledTransaction(transaction.getId());

        assertEquals(StatusTransactionEnum.PENDING, transaction.getStatus());
        verifyNoInteractions(transactionRepository, transactionEventsService);
    }

    @Test
    @DisplayName("Deve voltar para PENDING e salvar quando a publicação do evento falhar")
    void shouldRevertToPendingWhenPublishFails() {
        Transaction transaction = pending(LocalDateTime.now().minusMinutes(1));
        when(transactionSelector.getTransactionByIdInternal(transaction.getId())).thenReturn(transaction);
        doThrow(new RuntimeException("kafka indisponível"))
                .when(transactionEventsService).publishTransactionCreatedEvent(transaction);

        assertDoesNotThrow(() -> transactionExecutionService.executeScheduledTransaction(transaction.getId()));

        assertEquals(StatusTransactionEnum.PENDING, transaction.getStatus());
        verify(transactionRepository, times(2)).save(transaction);
    }
}
