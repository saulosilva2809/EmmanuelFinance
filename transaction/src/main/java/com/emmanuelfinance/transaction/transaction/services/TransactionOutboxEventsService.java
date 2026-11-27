package com.emmanuelfinance.transaction.transaction.services;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.outbox.TransactionOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionOutboxEventsService {

    private final TransactionOutbox transactionOutbox;

    public void publishTransactionCreatedEventIfPaid(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            transactionOutbox.transactionCreatedEvent(new TransactionCreatedEvent(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getCreditCardId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getInstallmentsCount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now(),
                    UUID.randomUUID()
            ));
        }
    }

    public void publishInvoicePaymentCreatedEventIfPaid(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            transactionOutbox.invoicePaymentCreatedEvent(new TransactionCreatedEvent(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getCreditCardId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getInstallmentsCount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now(),
                    UUID.randomUUID()
            ));
        }
    }

    public void publishTransactionUpdatedEventIfPaid(
            Transaction transaction,
            UUID oldAccountId,
            BigDecimal oldAmount,
            TypeTransactionEnum oldType,
            StatusTransactionEnum oldStatus
    ) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            transactionOutbox.transactionUpdatedEvent(new TransactionUpdatedEvent(
                    transaction.getId(),
                    oldAccountId,
                    transaction.getAccountId(),
                    transaction.getUserId(),
                    oldAmount,
                    transaction.getAmount(),
                    oldType,
                    transaction.getType(),
                    oldStatus,
                    transaction.getStatus(),
                    UUID.randomUUID()
            ));
        }
    }

    public void publishTransactionDeletedEventIfPaid(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            transactionOutbox.transactionDeletedEvent(new TransactionDeletedAndRestoreEvent(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getCreditCardId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getInstallmentsCount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now(),
                    UUID.randomUUID()
            ));
        }
    }

    public void publishTransactionRestoreEventIfPaid(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            transactionOutbox.transactionRestoredEvent(new TransactionDeletedAndRestoreEvent(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getCreditCardId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getInstallmentsCount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now(),
                    UUID.randomUUID()
            ));
        }
    }
}