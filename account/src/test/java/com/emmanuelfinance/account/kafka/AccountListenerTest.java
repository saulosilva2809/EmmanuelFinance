package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferDeletedAndRestoreEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferUpdatedEventDTO;
import com.emmanuelfinance.account.services.AccountBalanceService;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountListenerTest {

    @Mock
    private AccountBalanceService accountBalanceService;

    @InjectMocks
    private AccountListener accountListener;

    private TransactionCreatedEvent createdEvent(UUID creditCardId) {
        return new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), creditCardId, UUID.randomUUID(),
                new BigDecimal("100.00"), 1, TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID()
        );
    }

    private TransactionDeletedAndRestoreEvent deletedEvent() {
        return new TransactionDeletedAndRestoreEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                new BigDecimal("100.00"), 1, TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID()
        );
    }

    private TransactionUpdatedEvent updatedEvent() {
        return new TransactionUpdatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, BigDecimal.ONE, TypeTransactionEnum.EXPENSE, TypeTransactionEnum.EXPENSE,
                StatusTransactionEnum.PAID, StatusTransactionEnum.PAID, UUID.randomUUID()
        );
    }

    @Nested
    @DisplayName("Cenários do handleTransactionCreated")
    class TransactionCreatedTests {

        @Test
        @DisplayName("Deve atualizar o saldo da conta para transação sem cartão")
        void shouldUpdateBalanceForAccountTransaction() {
            TransactionCreatedEvent event = createdEvent(null);

            accountListener.handleTransactionCreated(event);

            verify(accountBalanceService, times(1)).updateBalanceFromTransaction(event);
        }

        @Test
        @DisplayName("Deve ignorar compras no cartão, que não mexem no saldo da conta")
        void shouldIgnoreCardTransactions() {
            accountListener.handleTransactionCreated(createdEvent(UUID.randomUUID()));

            verifyNoInteractions(accountBalanceService);
        }

        @Test
        @DisplayName("Deve propagar a exceção para o Kafka tentar de novo")
        void shouldPropagateExceptions() {
            TransactionCreatedEvent event = createdEvent(null);
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromTransaction(event);

            assertThrows(RuntimeException.class, () -> accountListener.handleTransactionCreated(event));
        }
    }

    @Nested
    @DisplayName("Cenários do handleInvoicePaymentCreated")
    class InvoicePaymentCreatedTests {

        @Test
        @DisplayName("Deve debitar o saldo da conta mesmo quando o evento tiver o cartão (pagamento de fatura)")
        void shouldUpdateBalanceEvenWhenEventHasCreditCard() {
            TransactionCreatedEvent event = createdEvent(UUID.randomUUID());

            accountListener.handleInvoicePaymentCreated(event);

            verify(accountBalanceService, times(1)).updateBalanceFromTransaction(event);
        }

        @Test
        @DisplayName("Deve propagar a exceção para o Kafka tentar de novo")
        void shouldPropagateExceptions() {
            TransactionCreatedEvent event = createdEvent(UUID.randomUUID());
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromTransaction(event);

            assertThrows(RuntimeException.class, () -> accountListener.handleInvoicePaymentCreated(event));
        }
    }

    @Nested
    @DisplayName("Cenários dos demais eventos de transação")
    class OtherTransactionEventsTests {

        @Test
        @DisplayName("Deve atualizar o saldo quando a transação for alterada")
        void shouldHandleUpdated() {
            TransactionUpdatedEvent event = updatedEvent();

            accountListener.handleTransactionUpdated(event);

            verify(accountBalanceService, times(1)).updateBalanceFromUpdatedTransaction(event);
        }

        @Test
        @DisplayName("Deve estornar o saldo quando a transação for excluída")
        void shouldHandleDeleted() {
            TransactionDeletedAndRestoreEvent event = deletedEvent();

            accountListener.handleTransactionDeleted(event);

            verify(accountBalanceService, times(1)).updateBalanceFromDeletedTransaction(event);
        }

        @Test
        @DisplayName("Deve reaplicar o saldo quando a transação for restaurada")
        void shouldHandleRestore() {
            TransactionDeletedAndRestoreEvent event = deletedEvent();

            accountListener.handleTransactionRestore(event);

            verify(accountBalanceService, times(1)).updateBalanceFromRestoreTransaction(event);
        }

        @Test
        @DisplayName("Deve propagar as exceções nos eventos de alteração, exclusão e restauração")
        void shouldPropagateExceptions() {
            TransactionUpdatedEvent updatedEvent = updatedEvent();
            TransactionDeletedAndRestoreEvent event = deletedEvent();
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromUpdatedTransaction(updatedEvent);
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromDeletedTransaction(event);
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromRestoreTransaction(event);

            assertThrows(RuntimeException.class, () -> accountListener.handleTransactionUpdated(updatedEvent));
            assertThrows(RuntimeException.class, () -> accountListener.handleTransactionDeleted(event));
            assertThrows(RuntimeException.class, () -> accountListener.handleTransactionRestore(event));
        }
    }

    @Nested
    @DisplayName("Cenários do handleTransferCreated")
    class TransferCreatedTests {

        private final TransferCreatedEventDTO event = new TransferCreatedEventDTO(UUID.randomUUID(), 
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, UUID.randomUUID()
        );

        @Test
        @DisplayName("Deve atualizar o saldo das contas da transferência")
        void shouldUpdateBalanceFromTransfer() {
            accountListener.handleTransferCreated(event);

            verify(accountBalanceService, times(1)).updateBalanceFromTransfer(event);
        }

        @Test
        @DisplayName("Deve propagar a exceção para o Kafka tentar de novo")
        void shouldPropagateExceptions() {
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromTransfer(event);

            assertThrows(RuntimeException.class, () -> accountListener.handleTransferCreated(event));
        }
    }

    @Test
    @DisplayName("Não deve mexer no saldo ao excluir ou restaurar uma compra no cartão")
    void shouldIgnoreCardPurchasesOnDeleteAndRestore() {
        TransactionDeletedAndRestoreEvent cardEvent = new TransactionDeletedAndRestoreEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, 1,
                TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID());

        accountListener.handleTransactionDeleted(cardEvent);
        accountListener.handleTransactionRestore(cardEvent);

        verifyNoInteractions(accountBalanceService);
    }

    @Test
    @DisplayName("Deve repassar a alteração, a exclusão e a restauração de transferência")
    void shouldHandleTheOtherTransferEvents() {
        TransferUpdatedEventDTO updated = new TransferUpdatedEventDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ONE,
                UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, UUID.randomUUID());
        TransferDeletedAndRestoreEventDTO event = new TransferDeletedAndRestoreEventDTO(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, UUID.randomUUID());

        accountListener.handleTransferUpdated(updated);
        accountListener.handleTransferDeleted(event);
        accountListener.handleTransferRestore(event);

        verify(accountBalanceService).updateBalanceFromUpdatedTransfer(updated);
        verify(accountBalanceService).updateBalanceFromDeletedTransfer(event);
        verify(accountBalanceService).updateBalanceFromRestoreTransfer(event);
    }
}
