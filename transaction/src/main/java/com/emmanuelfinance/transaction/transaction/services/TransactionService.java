package com.emmanuelfinance.transaction.transaction.services;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.transaction.dtos.UpdateTransactionDTO;
import com.emmanuelfinance.transaction.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.transaction.exceptions.TransactionErrorCode;
import com.emmanuelfinance.transaction.transaction.outbox.OutboxService;
import com.emmanuelfinance.transaction.transaction.services.scheduler.TransactionSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;
    private final TransactionMapper transactionMapper;
    private final TransactionSelector transactionSelector;
    private final IdempotencyService idempotencyService;
    private final TransactionValidatorService transactionValidatorService;
    private final TransactionEventsService transactionEventsService;
    private final TransactionSchedulerService transactionSchedulerService;
    private final OutboxService outboxService;

    // transação agendada espera a data (pendente); a que não é agendada já nasce paga
    private void setTransactionStatus(Transaction transaction) {
        if (Boolean.TRUE.equals(transaction.isScheduled())) {
            transaction.setStatus(StatusTransactionEnum.PENDING);
        } else {
            transaction.setStatus(StatusTransactionEnum.PAID);
        }
    }

    @Transactional
    public ResponseTransactionDTO create(CreateTransactionDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        transactionValidatorService.create.validate(data);

        String idempotencyKey = idempotencyService.generateIdempotencyKey(userId, data);
        idempotencyService.validateAndLock(idempotencyKey);

        Transaction transaction = transactionMapper.toEntity(data);
        transaction.setUserId(userId);
        transaction.setIdempotencyKey(idempotencyKey);
        setTransactionStatus(transaction);

        Transaction savedTransaction = transactionRepository.save(transaction);
        outboxService.transactionChanged(savedTransaction);

        if (savedTransaction.isScheduled()) {
            transactionSchedulerService.schedule(savedTransaction.getId(), savedTransaction.getDate());
        }

        // só transação paga avisa os outros serviços; a agendada avisa quando for executada
        transactionEventsService.publishTransactionCreatedEvent(savedTransaction);
        return transactionMapper.toResponseDTO(savedTransaction);
    }

    @Transactional
    public ResponseTransactionDTO update(UUID transactionId, UpdateTransactionDTO data) {
        Transaction transaction = transactionSelector.getTransactionById(transactionId);

        // compra no cartão mexe em limite e fatura, então não pode ser alterada
        if (transaction.getCreditCardId() != null) {
            throw new TransactionDomainException(TransactionErrorCode.CARD_TRANSACTIONS_CANNOT_BE_CHANGED);
        }

        // guarda os valores de antes, porque o aviso de atualização leva o antes e o depois
        UUID oldAccountId = transaction.getAccountId();
        BigDecimal oldAmount = transaction.getAmount();
        TypeEnum oldType = transaction.getType();
        StatusTransactionEnum oldStatus = transaction.getStatus();

        transactionValidatorService.update.validate(transaction, data);

        transactionMapper.updateEntityFromDTO(data, transaction);

        // valida se desativou o agendamento
        if (!transaction.isScheduled()) {
            transaction.setDate(null);
            transaction.setStatus(StatusTransactionEnum.PAID);

            transactionSchedulerService.cancel(transaction.getId());
        }

        Transaction updatedTransaction = transactionRepository.saveAndFlush(transaction);
        outboxService.transactionChanged(updatedTransaction);

        // valida se está agendado e continua pendente
        if (StatusTransactionEnum.PENDING.equals(transaction.getStatus()) && updatedTransaction.isScheduled()) {
            transactionSchedulerService.schedule(updatedTransaction.getId(), updatedTransaction.getDate());
        }

        transactionEventsService.publishTransactionUpdatedEvent(
                updatedTransaction,
                oldAccountId,
                oldAmount,
                oldType,
                oldStatus
        );

        return transactionMapper.toResponseDTO(updatedTransaction);
    }

    @Transactional
    public void delete(UUID transactionId) {
        Transaction transaction = transactionSelector.getTransactionById(transactionId);
        transactionRepository.delete(transaction);
        outboxService.transactionDeleted(transaction);

        transactionEventsService.publishTransactionDeletedEvent(transaction);
    }

    @Transactional
    public void restore(UUID transactionId) {
        Transaction transaction = transactionSelector.getTransactionByIdIncluingDeleted(transactionId);
        transactionValidatorService.restore.validate(transaction);

        transaction.setDeleted(false);
        transactionRepository.saveAndFlush(transaction);
        outboxService.transactionChanged(transaction);

        transactionEventsService.publishTransactionRestoreEvent(transaction);
    }
}
