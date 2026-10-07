package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.kafka.TransactionProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionEventsServiceTest {

    @Mock
    private TransactionProducer transactionProducer;

    @InjectMocks
    private TransactionEventsService transactionEventsService;

    private Transaction transaction(StatusTransactionEnum status, LocalDateTime date) {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());
        transaction.setStatus(status);
        transaction.setDate(date);
        return transaction;
    }

    @Nested
    @DisplayName("Cenários do publishTransactionCreatedEvent")
    class CreatedTests {

        @Test
        @DisplayName("Deve publicar o evento com os dados da transação quando estiver PAID")
        void shouldPublishWhenPaid() {
            LocalDateTime date = LocalDateTime.of(2026, 10, 5, 10, 0);
            Transaction transaction = transaction(StatusTransactionEnum.PAID, date);

            transactionEventsService.publishTransactionCreatedEvent(transaction);

            ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
            verify(transactionProducer, times(1)).publishTransactionCreated(captor.capture());
            TransactionCreatedEvent event = captor.getValue();

            assertEquals(transaction.getId(), event.transactionId());
            assertEquals(transaction.getAccountId(), event.accountId());
            assertEquals(transaction.getCreditCardId(), event.creditCardId());
            assertEquals(transaction.getUserId(), event.userId());
            assertEquals(transaction.getAmount(), event.amount());
            assertEquals(transaction.getInstallmentsCount(), event.installmentsCount());
            assertEquals(TypeEnum.EXPENSE, event.type());
            assertEquals(StatusTransactionEnum.PAID, event.status());
            assertEquals(date, event.date());
        }

        @Test
        @DisplayName("Deve usar a data atual quando a transação não tiver data")
        void shouldUseNowWhenDateIsNull() {
            Transaction transaction = transaction(StatusTransactionEnum.PAID, null);
            LocalDateTime before = LocalDateTime.now();

            transactionEventsService.publishTransactionCreatedEvent(transaction);

            ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
            verify(transactionProducer).publishTransactionCreated(captor.capture());
            assertNotNull(captor.getValue().date());
            assertFalse(captor.getValue().date().isBefore(before));
        }

        @Test
        @DisplayName("Não deve publicar quando a transação estiver PENDING ou FAILED")
        void shouldNotPublishWhenNotPaid() {
            transactionEventsService.publishTransactionCreatedEvent(transaction(StatusTransactionEnum.PENDING, null));
            transactionEventsService.publishTransactionCreatedEvent(transaction(StatusTransactionEnum.FAILED, null));

            verifyNoInteractions(transactionProducer);
        }
    }

    @Nested
    @DisplayName("Cenários do publishTransactionUpdatedEvent")
    class UpdatedTests {

        @Test
        @DisplayName("Deve publicar o evento com os valores antigos e novos quando estiver PAID")
        void shouldPublishWithOldAndNewValues() {
            Transaction transaction = transaction(StatusTransactionEnum.PAID, null);
            UUID oldAccountId = UUID.randomUUID();
            BigDecimal oldAmount = new BigDecimal("50.00");

            transactionEventsService.publishTransactionUpdatedEvent(
                    transaction, oldAccountId, oldAmount, TypeEnum.INCOME, StatusTransactionEnum.PENDING
            );

            ArgumentCaptor<TransactionUpdatedEvent> captor = ArgumentCaptor.forClass(TransactionUpdatedEvent.class);
            verify(transactionProducer, times(1)).publishTransactionUpdated(captor.capture());
            TransactionUpdatedEvent event = captor.getValue();

            assertEquals(transaction.getId(), event.transactionId());
            assertEquals(oldAccountId, event.oldAccountId());
            assertEquals(transaction.getAccountId(), event.newAccountId());
            assertEquals(transaction.getUserId(), event.userId());
            assertEquals(oldAmount, event.oldAmount());
            assertEquals(transaction.getAmount(), event.newAmount());
            assertEquals(TypeEnum.INCOME, event.oldType());
            assertEquals(TypeEnum.EXPENSE, event.newType());
            assertEquals(StatusTransactionEnum.PENDING, event.oldStatus());
            assertEquals(StatusTransactionEnum.PAID, event.newStatus());
        }

        @Test
        @DisplayName("Não deve publicar quando a transação não estiver PAID")
        void shouldNotPublishWhenNotPaid() {
            transactionEventsService.publishTransactionUpdatedEvent(
                    transaction(StatusTransactionEnum.PENDING, null),
                    UUID.randomUUID(), BigDecimal.TEN, TypeEnum.EXPENSE, StatusTransactionEnum.PENDING
            );

            verifyNoInteractions(transactionProducer);
        }
    }

    @Nested
    @DisplayName("Cenários do publishTransactionDeletedEvent")
    class DeletedTests {

        @Test
        @DisplayName("Deve publicar o evento de exclusão quando estiver PAID")
        void shouldPublishWhenPaid() {
            LocalDateTime date = LocalDateTime.of(2026, 10, 5, 10, 0);
            Transaction transaction = transaction(StatusTransactionEnum.PAID, date);

            transactionEventsService.publishTransactionDeletedEvent(transaction);

            ArgumentCaptor<TransactionDeletedAndRestoreEvent> captor =
                    ArgumentCaptor.forClass(TransactionDeletedAndRestoreEvent.class);
            verify(transactionProducer, times(1)).publishTransactionDeleted(captor.capture());
            assertEquals(transaction.getId(), captor.getValue().transactionId());
            assertEquals(transaction.getAmount(), captor.getValue().amount());
            assertEquals(date, captor.getValue().date());
        }

        @Test
        @DisplayName("Não deve publicar quando a transação não estiver PAID")
        void shouldNotPublishWhenNotPaid() {
            transactionEventsService.publishTransactionDeletedEvent(transaction(StatusTransactionEnum.PENDING, null));

            verifyNoInteractions(transactionProducer);
        }
    }

    @Nested
    @DisplayName("Cenários do publishTransactionRestoreEvent")
    class RestoreTests {

        @Test
        @DisplayName("Deve publicar o evento de restauração quando estiver PAID")
        void shouldPublishWhenPaid() {
            Transaction transaction = transaction(StatusTransactionEnum.PAID, null);

            transactionEventsService.publishTransactionRestoreEvent(transaction);

            ArgumentCaptor<TransactionDeletedAndRestoreEvent> captor =
                    ArgumentCaptor.forClass(TransactionDeletedAndRestoreEvent.class);
            verify(transactionProducer, times(1)).publishTransactionRestore(captor.capture());
            assertEquals(transaction.getId(), captor.getValue().transactionId());
            assertEquals(transaction.getUserId(), captor.getValue().userId());
            assertNotNull(captor.getValue().date());
        }

        @Test
        @DisplayName("Não deve publicar quando a transação não estiver PAID")
        void shouldNotPublishWhenNotPaid() {
            transactionEventsService.publishTransactionRestoreEvent(transaction(StatusTransactionEnum.FAILED, null));

            verifyNoInteractions(transactionProducer);
        }
    }

    @Nested
    @DisplayName("Cenários do publishInvoicePaymentCreatedEvent")
    class InvoicePaymentCreatedTests {

        @Test
        @DisplayName("Deve publicar no tópico de pagamento de fatura com os dados da transação quando estiver PAID")
        void shouldPublishInvoicePaymentWhenPaid() {
            Transaction transaction = transaction(StatusTransactionEnum.PAID, LocalDateTime.of(2026, 10, 5, 10, 0));
            transaction.setCreditCardId(UUID.randomUUID());

            transactionEventsService.publishInvoicePaymentCreatedEvent(transaction);

            ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
            verify(transactionProducer, times(1)).publishInvoicePaymentCreated(captor.capture());
            verify(transactionProducer, never()).publishTransactionCreated(any());

            TransactionCreatedEvent event = captor.getValue();
            assertEquals(transaction.getId(), event.transactionId());
            assertEquals(transaction.getAccountId(), event.accountId());
            assertEquals(transaction.getCreditCardId(), event.creditCardId());
            assertEquals(transaction.getUserId(), event.userId());
            assertEquals(transaction.getAmount(), event.amount());
            assertEquals(TypeEnum.EXPENSE, event.type());
            assertEquals(StatusTransactionEnum.PAID, event.status());
        }

        @Test
        @DisplayName("Não deve publicar quando a transação não estiver PAID")
        void shouldNotPublishWhenNotPaid() {
            transactionEventsService.publishInvoicePaymentCreatedEvent(transaction(StatusTransactionEnum.PENDING, null));
            transactionEventsService.publishInvoicePaymentCreatedEvent(transaction(StatusTransactionEnum.FAILED, null));

            verifyNoInteractions(transactionProducer);
        }
    }
}
