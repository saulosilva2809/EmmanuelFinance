package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.transaction.outbox.OutboxService;
import com.emmanuelfinance.transaction.transaction.services.TransactionOutboxEventsService;
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
public class TransactionOutboxEventsServiceTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TransactionOutboxEventsService transactionOutboxEventsService;

    private Transaction transaction(StatusTransactionEnum status, LocalDateTime date) {
        Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());
        transaction.setStatus(status);
        transaction.setDate(date);
        return transaction;
    }

    @Nested
    @DisplayName("Cenários do publishTransactionCreatedEventIfPaid")
    class CreatedTests {

        @Test
        @DisplayName("Deve publicar o evento com os dados da transação quando estiver PAID")
        void shouldPublishWhenPaid() {
            LocalDateTime date = LocalDateTime.of(2026, 10, 5, 10, 0);
            Transaction transaction = transaction(StatusTransactionEnum.PAID, date);

            transactionOutboxEventsService.publishTransactionCreatedEventIfPaid(transaction);

            ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
            verify(outboxService, times(1)).transactionCreatedEvent(captor.capture());
            TransactionCreatedEvent event = captor.getValue();

            assertEquals(transaction.getId(), event.transactionId());
            assertEquals(transaction.getAccountId(), event.accountId());
            assertEquals(transaction.getCreditCardId(), event.creditCardId());
            assertEquals(transaction.getUserId(), event.userId());
            assertEquals(transaction.getAmount(), event.amount());
            assertEquals(transaction.getInstallmentsCount(), event.installmentsCount());
            assertEquals(TypeTransactionEnum.EXPENSE, event.type());
            assertEquals(StatusTransactionEnum.PAID, event.status());
            assertEquals(date, event.date());
        }

        @Test
        @DisplayName("Deve usar a data atual quando a transação não tiver data")
        void shouldUseNowWhenDateIsNull() {
            Transaction transaction = transaction(StatusTransactionEnum.PAID, null);
            LocalDateTime before = LocalDateTime.now();

            transactionOutboxEventsService.publishTransactionCreatedEventIfPaid(transaction);

            ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
            verify(outboxService).transactionCreatedEvent(captor.capture());
            assertNotNull(captor.getValue().date());
            assertFalse(captor.getValue().date().isBefore(before));
        }

        @Test
        @DisplayName("Não deve publicar quando a transação estiver PENDING ou FAILED")
        void shouldNotPublishWhenNotPaid() {
            transactionOutboxEventsService.publishTransactionCreatedEventIfPaid(transaction(StatusTransactionEnum.PENDING, null));
            transactionOutboxEventsService.publishTransactionCreatedEventIfPaid(transaction(StatusTransactionEnum.FAILED, null));

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    @DisplayName("Cenários do publishTransactionUpdatedEventIfPaid")
    class UpdatedTests {

        @Test
        @DisplayName("Deve publicar o evento com os valores antigos e novos quando estiver PAID")
        void shouldPublishWithOldAndNewValues() {
            Transaction transaction = transaction(StatusTransactionEnum.PAID, null);
            UUID oldAccountId = UUID.randomUUID();
            BigDecimal oldAmount = new BigDecimal("50.00");

            transactionOutboxEventsService.publishTransactionUpdatedEventIfPaid(
                    transaction, oldAccountId, oldAmount, TypeTransactionEnum.INCOME, StatusTransactionEnum.PENDING
            );

            ArgumentCaptor<TransactionUpdatedEvent> captor = ArgumentCaptor.forClass(TransactionUpdatedEvent.class);
            verify(outboxService, times(1)).transactionUpdatedEvent(captor.capture());
            TransactionUpdatedEvent event = captor.getValue();

            assertEquals(transaction.getId(), event.transactionId());
            assertEquals(oldAccountId, event.oldAccountId());
            assertEquals(transaction.getAccountId(), event.newAccountId());
            assertEquals(transaction.getUserId(), event.userId());
            assertEquals(oldAmount, event.oldAmount());
            assertEquals(transaction.getAmount(), event.newAmount());
            assertEquals(TypeTransactionEnum.INCOME, event.oldType());
            assertEquals(TypeTransactionEnum.EXPENSE, event.newType());
            assertEquals(StatusTransactionEnum.PENDING, event.oldStatus());
            assertEquals(StatusTransactionEnum.PAID, event.newStatus());
        }

        @Test
        @DisplayName("Não deve publicar quando a transação não estiver PAID")
        void shouldNotPublishWhenNotPaid() {
            transactionOutboxEventsService.publishTransactionUpdatedEventIfPaid(
                    transaction(StatusTransactionEnum.PENDING, null),
                    UUID.randomUUID(), BigDecimal.TEN, TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PENDING
            );

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    @DisplayName("Cenários do publishTransactionDeletedEventIfPaid")
    class DeletedTests {

        @Test
        @DisplayName("Deve publicar o evento de exclusão quando estiver PAID")
        void shouldPublishWhenPaid() {
            LocalDateTime date = LocalDateTime.of(2026, 10, 5, 10, 0);
            Transaction transaction = transaction(StatusTransactionEnum.PAID, date);

            transactionOutboxEventsService.publishTransactionDeletedEventIfPaid(transaction);

            ArgumentCaptor<TransactionDeletedAndRestoreEvent> captor =
                    ArgumentCaptor.forClass(TransactionDeletedAndRestoreEvent.class);
            verify(outboxService, times(1)).transactionDeletedEvent(captor.capture());
            assertEquals(transaction.getId(), captor.getValue().transactionId());
            assertEquals(transaction.getAmount(), captor.getValue().amount());
            assertEquals(date, captor.getValue().date());
        }

        @Test
        @DisplayName("Não deve publicar quando a transação não estiver PAID")
        void shouldNotPublishWhenNotPaid() {
            transactionOutboxEventsService.publishTransactionDeletedEventIfPaid(transaction(StatusTransactionEnum.PENDING, null));

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    @DisplayName("Cenários do publishTransactionRestoreEventIfPaid")
    class RestoreTests {

        @Test
        @DisplayName("Deve publicar o evento de restauração quando estiver PAID")
        void shouldPublishWhenPaid() {
            Transaction transaction = transaction(StatusTransactionEnum.PAID, null);

            transactionOutboxEventsService.publishTransactionRestoreEventIfPaid(transaction);

            ArgumentCaptor<TransactionDeletedAndRestoreEvent> captor =
                    ArgumentCaptor.forClass(TransactionDeletedAndRestoreEvent.class);
            verify(outboxService, times(1)).transactionRestoredEvent(captor.capture());
            assertEquals(transaction.getId(), captor.getValue().transactionId());
            assertEquals(transaction.getUserId(), captor.getValue().userId());
            assertNotNull(captor.getValue().date());
        }

        @Test
        @DisplayName("Não deve publicar quando a transação não estiver PAID")
        void shouldNotPublishWhenNotPaid() {
            transactionOutboxEventsService.publishTransactionRestoreEventIfPaid(transaction(StatusTransactionEnum.FAILED, null));

            verifyNoInteractions(outboxService);
        }
    }

    @Nested
    @DisplayName("Cenários do publishInvoicePaymentCreatedEventIfPaid")
    class InvoicePaymentCreatedTests {

        @Test
        @DisplayName("Deve publicar no tópico de pagamento de fatura com os dados da transação quando estiver PAID")
        void shouldPublishInvoicePaymentWhenPaid() {
            Transaction transaction = transaction(StatusTransactionEnum.PAID, LocalDateTime.of(2026, 10, 5, 10, 0));
            transaction.setCreditCardId(UUID.randomUUID());

            transactionOutboxEventsService.publishInvoicePaymentCreatedEventIfPaid(transaction);

            ArgumentCaptor<TransactionCreatedEvent> captor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
            verify(outboxService, times(1)).invoicePaymentCreatedEvent(captor.capture());
            verify(outboxService, never()).transactionCreatedEvent(any());

            TransactionCreatedEvent event = captor.getValue();
            assertEquals(transaction.getId(), event.transactionId());
            assertEquals(transaction.getAccountId(), event.accountId());
            assertEquals(transaction.getCreditCardId(), event.creditCardId());
            assertEquals(transaction.getUserId(), event.userId());
            assertEquals(transaction.getAmount(), event.amount());
            assertEquals(TypeTransactionEnum.EXPENSE, event.type());
            assertEquals(StatusTransactionEnum.PAID, event.status());
        }

        @Test
        @DisplayName("Não deve publicar quando a transação não estiver PAID")
        void shouldNotPublishWhenNotPaid() {
            transactionOutboxEventsService.publishInvoicePaymentCreatedEventIfPaid(transaction(StatusTransactionEnum.PENDING, null));
            transactionOutboxEventsService.publishInvoicePaymentCreatedEventIfPaid(transaction(StatusTransactionEnum.FAILED, null));

            verifyNoInteractions(outboxService);
        }
    }
}
