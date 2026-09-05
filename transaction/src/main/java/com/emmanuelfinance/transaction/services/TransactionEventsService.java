package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.kafka.TransactionProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionEventsService {

    private final TransactionProducer transactionProducer;

    public void publishTransactionCreatedEvent(Transaction transaction) {
        if (transaction.getStatus() == StatusTransactionEnum.PAID) {
            transactionProducer.publishTransactionCreated(new TransactionCreatedEvent(
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
            transactionProducer.publishTransactionUpdated(new TransactionUpdatedEvent(
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
            transactionProducer.publishTransactionDeleted(new TransactionDeletedAndRestoreEvent(
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
            transactionProducer.publishTransactionRestore(new TransactionDeletedAndRestoreEvent(
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