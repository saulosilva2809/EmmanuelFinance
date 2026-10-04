package com.emmanuelfinance.creditcard.creditcard.kafka;

import com.emmanuelfinance.creditcard.invoice.InvoiceTestDataBuilder;
import com.emmanuelfinance.creditcard.creditcard.services.CreditCardBalanceKafkaService;
import com.emmanuelfinance.creditcard.creditcard.services.CreditCardInternalService;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
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
public class CreditCardListenerTest {

    @Mock
    private CreditCardInternalService creditCardInternalService;

    @Mock
    private CreditCardBalanceKafkaService creditCardBalanceKafkaService;

    @Mock
    private CreditCardProducer creditCardProducer;

    @InjectMocks
    private CreditCardListener creditCardListener;

    private final UUID userId = UUID.randomUUID();

    @Nested
    @DisplayName("Cenários do handleAccountDeleted")
    class AccountDeletedTests {

        @Test
        @DisplayName("Deve desativar os cartões da conta quando o evento for DELETED")
        void shouldDeactivateCardsWhenAccountIsDeleted() {
            UUID accountId = UUID.randomUUID();

            creditCardListener.handleAccountDeleted(new AccountEventDTO(accountId, userId, StatusEventEnum.DELETED));

            verify(creditCardInternalService, times(1)).deactivateCardsByAccountId(accountId);
        }

        @Test
        @DisplayName("Deve ignorar eventos de conta que não sejam DELETED")
        void shouldIgnoreOtherStatuses() {
            creditCardListener.handleAccountDeleted(
                    new AccountEventDTO(UUID.randomUUID(), userId, StatusEventEnum.CREATED)
            );
            creditCardListener.handleAccountDeleted(
                    new AccountEventDTO(UUID.randomUUID(), userId, StatusEventEnum.RESTORE)
            );

            verifyNoInteractions(creditCardInternalService);
        }

        @Test
        @DisplayName("Não deve propagar a exceção ao falhar ao desativar os cartões")
        void shouldSwallowExceptions() {
            UUID accountId = UUID.randomUUID();
            doThrow(new RuntimeException("falha")).when(creditCardInternalService).deactivateCardsByAccountId(accountId);

            assertDoesNotThrow(() ->
                    creditCardListener.handleAccountDeleted(new AccountEventDTO(accountId, userId, StatusEventEnum.DELETED))
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
            verifyNoInteractions(creditCardProducer);
        }

        @Test
        @DisplayName("Deve ignorar a transação sem cartão")
        void shouldIgnoreTransactionWithoutCard() {
            creditCardListener.handleTransactionCreated(event(null));

            verifyNoInteractions(creditCardBalanceKafkaService, creditCardProducer);
        }

        @Test
        @DisplayName("Deve publicar transaction-failed quando o processamento falhar")
        void shouldPublishFailedWhenProcessingFails() {
            TransactionCreatedEvent event = event(UUID.randomUUID());
            doThrow(new RuntimeException("falha")).when(creditCardBalanceKafkaService).processTransactionCreate(event);

            assertDoesNotThrow(() -> creditCardListener.handleTransactionCreated(event));

            verify(creditCardProducer, times(1)).publishTransactionFailed(event.transactionId());
        }
    }

    @Nested
    @DisplayName("Cenários do handleTransactionDeleted")
    class TransactionDeletedTests {

        private final TransactionDeletedAndRestoreEvent event =
                InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(UUID.randomUUID());

        @Test
        @DisplayName("Deve processar a exclusão sem publicar falha")
        void shouldProcessDeletion() {
            creditCardListener.handleTransactionDeleted(event);

            verify(creditCardBalanceKafkaService, times(1)).processTransactionDeletion(event);
            verifyNoInteractions(creditCardProducer);
        }

        @Test
        @DisplayName("Deve publicar transaction-failed quando o processamento falhar")
        void shouldPublishFailedWhenProcessingFails() {
            doThrow(new RuntimeException("falha")).when(creditCardBalanceKafkaService).processTransactionDeletion(event);

            assertDoesNotThrow(() -> creditCardListener.handleTransactionDeleted(event));

            verify(creditCardProducer, times(1)).publishTransactionFailed(event.transactionId());
        }
    }

    @Nested
    @DisplayName("Cenários do handleTransactionRestore")
    class TransactionRestoreTests {

        private final TransactionDeletedAndRestoreEvent event =
                InvoiceTestDataBuilder.transactionDeletedAndRestoreEvent(UUID.randomUUID());

        @Test
        @DisplayName("Deve processar a restauração sem publicar falha")
        void shouldProcessRestore() {
            creditCardListener.handleTransactionRestore(event);

            verify(creditCardBalanceKafkaService, times(1)).processTransactionRestore(event);
            verifyNoInteractions(creditCardProducer);
        }

        @Test
        @DisplayName("Deve publicar transaction-failed quando a restauração falhar")
        void shouldPublishFailedWhenProcessingFails() {
            doThrow(new RuntimeException("limite insuficiente")).when(creditCardBalanceKafkaService)
                    .processTransactionRestore(event);

            assertDoesNotThrow(() -> creditCardListener.handleTransactionRestore(event));

            verify(creditCardProducer, times(1)).publishTransactionFailed(event.transactionId());
        }
    }
}
