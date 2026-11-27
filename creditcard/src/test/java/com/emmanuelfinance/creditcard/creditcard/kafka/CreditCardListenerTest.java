package com.emmanuelfinance.creditcard.creditcard.kafka;

import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.creditcard.services.CreditCardBalanceKafkaService;
import com.emmanuelfinance.creditcard.creditcard.services.CreditCardInternalService;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.enums.StatusEventEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
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
public class CreditCardListenerTest {

    @Mock
    private CreditCardInternalService creditCardInternalService;

    @Mock
    private CreditCardBalanceKafkaService creditCardBalanceKafkaService;

    @InjectMocks
    private CreditCardListener creditCardListener;

    private final UUID userId = UUID.randomUUID();

    @Nested
    @DisplayName("Cenários do handleAccountEvent")
    class AccountDeletedTests {

        @Test
        @DisplayName("Deve desativar os cartões da conta quando o evento for DELETED")
        void shouldDeactivateCardsWhenAccountIsDeleted() {
            UUID accountId = UUID.randomUUID();

            creditCardListener.handleAccountEvent(new AccountEventDTO(accountId, userId, StatusEventEnum.DELETED));

            verify(creditCardInternalService, times(1)).deactivateCardsByAccountId(accountId);
        }

        @Test
        @DisplayName("Deve reativar os cartões quando a conta for restaurada")
        void shouldRestoreCardsWhenAccountIsRestored() {
            UUID accountId = UUID.randomUUID();

            creditCardListener.handleAccountEvent(new AccountEventDTO(accountId, userId, StatusEventEnum.RESTORE));

            verify(creditCardInternalService).restoreCardsByAccountId(accountId);
        }

        @Test
        @DisplayName("Deve ignorar o evento de conta criada")
        void shouldIgnoreCreatedAccounts() {
            creditCardListener.handleAccountEvent(
                    new AccountEventDTO(UUID.randomUUID(), userId, StatusEventEnum.CREATED)
            );

            verifyNoInteractions(creditCardInternalService);
        }

        @Test
        @DisplayName("Deve propagar a exceção para o Kafka tentar de novo")
        void shouldPropagateExceptions() {
            UUID accountId = UUID.randomUUID();
            doThrow(new RuntimeException("falha")).when(creditCardInternalService).deactivateCardsByAccountId(accountId);

            assertThrows(RuntimeException.class, () ->
                    creditCardListener.handleAccountEvent(new AccountEventDTO(accountId, userId, StatusEventEnum.DELETED))
            );
        }
    }

    @Nested
    @DisplayName("Cenários do handleTransactionCreated")
    class TransactionCreatedTests {

        private TransactionCreatedEvent event(UUID creditCardId) {
            return InvoiceTestDataBuilder.transactionCreatedEvent(
                    userId, creditCardId, new BigDecimal("100.00"), 1, LocalDateTime.now()
            );
        }

        @Test
        @DisplayName("Deve processar a criação quando a transação for de cartão")
        void shouldProcessCardTransaction() {
            TransactionCreatedEvent event = event(UUID.randomUUID());

            creditCardListener.handleTransactionCreated(event);

            verify(creditCardBalanceKafkaService, times(1)).processTransactionCreate(event);
        }

        @Test
        @DisplayName("Deve ignorar a transação sem cartão")
        void shouldIgnoreTransactionWithoutCard() {
            creditCardListener.handleTransactionCreated(event(null));

            verifyNoInteractions(creditCardBalanceKafkaService);
        }

        @Test
        @DisplayName("Deve propagar a exceção para o Kafka tentar de novo")
        void shouldPropagateExceptions() {
            TransactionCreatedEvent event = event(UUID.randomUUID());
            doThrow(new RuntimeException("falha")).when(creditCardBalanceKafkaService).processTransactionCreate(event);

            assertThrows(RuntimeException.class, () -> creditCardListener.handleTransactionCreated(event));
        }
    }

    @Nested
    @DisplayName("Cenários do handleTransactionDeleted")
    class TransactionDeletedTests {

        private final TransactionDeletedAndRestoreEvent event =
                InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(UUID.randomUUID());

        @Test
        @DisplayName("Deve processar a exclusão com cartão")
        void shouldProcessDeletion() {
            creditCardListener.handleTransactionDeleted(event);

            verify(creditCardBalanceKafkaService, times(1)).processTransactionDeletion(event);
        }

        @Test
        @DisplayName("Deve propagar a exceção para o Kafka tentar de novo")
        void shouldPropagateExceptions() {
            doThrow(new RuntimeException("falha")).when(creditCardBalanceKafkaService).processTransactionDeletion(event);

            assertThrows(RuntimeException.class, () -> creditCardListener.handleTransactionDeleted(event));
        }

        @Test
        @DisplayName("Deve ignorar a transação sem cartão")
        void shouldIgnoreTransactionWithoutCard() {
            creditCardListener.handleTransactionDeleted(new TransactionDeletedAndRestoreEvent(
                    UUID.randomUUID(), UUID.randomUUID(), null, userId, BigDecimal.TEN, 1,
                    TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID()
            ));

            verifyNoInteractions(creditCardBalanceKafkaService);
        }
    }

    @Nested
    @DisplayName("Cenários do handleTransactionRestore")
    class TransactionRestoreTests {

        private final TransactionDeletedAndRestoreEvent event =
                InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(UUID.randomUUID());

        @Test
        @DisplayName("Deve processar a restauração com cartão")
        void shouldProcessRestore() {
            creditCardListener.handleTransactionRestore(event);

            verify(creditCardBalanceKafkaService, times(1)).processTransactionRestore(event);
        }

        @Test
        @DisplayName("Deve propagar a exceção para o Kafka tentar de novo")
        void shouldPropagateExceptions() {
            doThrow(new RuntimeException("limite insuficiente")).when(creditCardBalanceKafkaService)
                    .processTransactionRestore(event);

            assertThrows(RuntimeException.class, () -> creditCardListener.handleTransactionRestore(event));
        }

        @Test
        @DisplayName("Deve ignorar a transação sem cartão")
        void shouldIgnoreTransactionWithoutCard() {
            creditCardListener.handleTransactionRestore(new TransactionDeletedAndRestoreEvent(
                    UUID.randomUUID(), UUID.randomUUID(), null, userId, BigDecimal.TEN, 1,
                    TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID()
            ));

            verifyNoInteractions(creditCardBalanceKafkaService);
        }
    }
}
