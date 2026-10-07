package com.emmanuelfinance.account.kafka;

import com.emmanuelfinance.account.services.AccountBalanceService;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
                new BigDecimal("100.00"), 1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now()
        );
    }

    private TransactionDeletedAndRestoreEvent deletedEvent() {
        return new TransactionDeletedAndRestoreEvent(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(),
                new BigDecimal("100.00"), 1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now()
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
        @DisplayName("Não deve propagar a exceção quando a atualização do saldo falhar")
        void shouldSwallowExceptions() {
            TransactionCreatedEvent event = createdEvent(null);
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromTransaction(event);

            assertDoesNotThrow(() -> accountListener.handleTransactionCreated(event));
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
        @DisplayName("Não deve propagar a exceção quando a atualização do saldo falhar")
        void shouldSwallowExceptions() {
            TransactionCreatedEvent event = createdEvent(UUID.randomUUID());
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromTransaction(event);

            assertDoesNotThrow(() -> accountListener.handleInvoicePaymentCreated(event));
        }
    }

    @Nested
    @DisplayName("Cenários dos demais eventos de transação")
    class OtherTransactionEventsTests {

        @Test
        @DisplayName("Deve atualizar o saldo quando a transação for alterada")
        void shouldHandleUpdated() {
            TransactionUpdatedEvent event = new TransactionUpdatedEvent(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    BigDecimal.TEN, BigDecimal.ONE, TypeEnum.EXPENSE, TypeEnum.EXPENSE,
                    StatusTransactionEnum.PAID, StatusTransactionEnum.PAID
            );

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
        @DisplayName("Não deve propagar exceções nos eventos de alteração, exclusão e restauração")
        void shouldSwallowExceptions() {
            TransactionDeletedAndRestoreEvent event = deletedEvent();
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromDeletedTransaction(event);
            doThrow(new RuntimeException("falha")).when(accountBalanceService).updateBalanceFromRestoreTransaction(event);

            assertDoesNotThrow(() -> accountListener.handleTransactionDeleted(event));
            assertDoesNotThrow(() -> accountListener.handleTransactionRestore(event));
        }
    }
}
