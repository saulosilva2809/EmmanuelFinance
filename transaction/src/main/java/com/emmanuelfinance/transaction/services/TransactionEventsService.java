package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionEventsService {

    private final OutboxService outboxService;

    public void publishTransactionCreatedEvent(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            outboxService.transactionCreatedEvent(new TransactionCreatedEvent(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getCreditCardId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getInstallmentsCount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now()
            ));
        }
    }

    public void publishInvoicePaymentCreatedEvent(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            outboxService.invoicePaymentCreatedEvent(new TransactionCreatedEvent(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getCreditCardId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getInstallmentsCount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now()
            ));
        }
    }

    public void publishTransactionUpdatedEvent(
            Transaction transaction,
            UUID oldAccountId,
            BigDecimal oldAmount,
            TypeEnum oldType,
            StatusTransactionEnum oldStatus
    ) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            outboxService.transactionUpdatedEvent(new TransactionUpdatedEvent(
                    transaction.getId(),
                    oldAccountId,
                    transaction.getAccountId(),
                    transaction.getUserId(),
                    oldAmount,
                    transaction.getAmount(),
                    oldType,
                    transaction.getType(),
                    oldStatus,
                    transaction.getStatus()
            ));
        }
    }

    public void publishTransactionDeletedEvent(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            outboxService.transactionDeletedEvent(new TransactionDeletedAndRestoreEvent(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getCreditCardId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getInstallmentsCount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now()
            ));
        }
    }

    public void publishTransactionRestoreEvent(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            outboxService.transactionRestoredEvent(new TransactionDeletedAndRestoreEvent(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getCreditCardId(),
                    transaction.getUserId(),
                    transaction.getAmount(),
                    transaction.getInstallmentsCount(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getDate() != null ? transaction.getDate() : LocalDateTime.now()
            ));
        }
    }
}