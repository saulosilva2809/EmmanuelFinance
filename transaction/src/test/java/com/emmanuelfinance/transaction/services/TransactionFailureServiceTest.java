package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.FailedOperationEnum;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentFailedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionFailedEvent;
import com.emmanuelfinance.transaction.TransactionTestDataBuilder;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.transaction.outbox.TransactionOutbox;
import com.emmanuelfinance.transaction.transaction.services.TransactionFailureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionFailureServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionOutbox transactionOutbox;

    @InjectMocks
    private TransactionFailureService transactionFailureService;

    private final Transaction transaction = TransactionTestDataBuilder.transactionEntity(UUID.randomUUID());

    private void compensate(FailedOperationEnum operation) {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        transactionFailureService.compensate(
                new TransactionFailedEvent(transaction.getId(), operation, null, null, null, null));
    }

    @Test
    void shouldMarkAFailedCreation() {
        compensate(FailedOperationEnum.CREATE);

        assertEquals(StatusTransactionEnum.FAILED, transaction.getStatus());
        verify(transactionRepository).saveAndFlush(transaction);
        verify(transactionOutbox).transactionChanged(transaction);
        verify(transactionOutbox, never()).invoicePaymentFailedEvent(any());
    }

    @Test
    void shouldAskTheCardToRevertAFailedInvoicePaymentOnlyOnce() {
        transaction.setCreditCardId(UUID.randomUUID());
        transaction.setInvoiceId(UUID.randomUUID());

        compensate(FailedOperationEnum.CREATE);
        compensate(FailedOperationEnum.CREATE);

        verify(transactionOutbox, times(1)).invoicePaymentFailedEvent(new InvoicePaymentFailedEvent(
                transaction.getId(), transaction.getCreditCardId(), transaction.getInvoiceId(), transaction.getAmount()));
    }

    @Test
    void shouldUndoAFailedDeletion() {
        transaction.setDeleted(true);

        compensate(FailedOperationEnum.DELETE);

        assertFalse(transaction.isDeleted());
        assertEquals(StatusTransactionEnum.PAID, transaction.getStatus());
        verify(transactionOutbox).transactionChanged(transaction);
    }

    @Test
    void shouldDeleteAgainAfterAFailedRestore() {
        compensate(FailedOperationEnum.RESTORE);

        assertTrue(transaction.isDeleted());
        verify(transactionOutbox).transactionDeleted(transaction);
    }

    @Test
    void shouldGoBackToTheOldValuesAfterAFailedUpdate() {
        UUID oldAccount = UUID.randomUUID();
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        transactionFailureService.compensate(new TransactionFailedEvent(
                transaction.getId(), FailedOperationEnum.UPDATE, oldAccount, BigDecimal.ONE,
                TypeTransactionEnum.INCOME, StatusTransactionEnum.PAID));

        assertEquals(oldAccount, transaction.getAccountId());
        assertEquals(BigDecimal.ONE, transaction.getAmount());
        assertEquals(TypeTransactionEnum.INCOME, transaction.getType());
        assertEquals(StatusTransactionEnum.PAID, transaction.getStatus());
    }
}
