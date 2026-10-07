package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceInternal {

    private final TransactionSelector transactionSelector;
    private final CategoryClientCacheService categoryClientCacheService;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;
    private final TransactionEventsService transactionEventsService;
    private final IdempotencyService idempotencyService;

    public TransactionSummaryDTO getSummaryDTO(UUID transactionId) {
        Transaction transaction = transactionSelector.getTransactionByIdInternal(
                transactionId
        );

        // transações de pagamento de fatura não têm categoria
        CategorySummaryDTO category = transaction.getCategoryId() != null
                ? categoryClientCacheService.getCategorySummaryDTO(transaction.getCategoryId())
                : null;

        return new TransactionSummaryDTO(
                category,
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getType()
        );
    }

    /**
     * O evento só sai depois do commit, para o saldo da conta não ser debitado se a transação não for salva.
     */
    private void publishInvoicePaymentAfterCommit(Transaction transaction) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            transactionEventsService.publishInvoicePaymentCreatedEvent(transaction);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                transactionEventsService.publishInvoicePaymentCreatedEvent(transaction);
            }
        });
    }

    @Transactional
    public void createTransactionRelatedToInvoicePayment(InvoicePaymentDTO data) {
        String idempotencyKey = idempotencyService.generateInvoicePaymentKey(data.userId(), data.invoiceId());

        if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("Pagamento da fatura {} já processado, evento ignorado.", data.invoiceId());
            return;
        }

        String description = "Transação referente ao pagamento da fatura do mês %02d/%d do cartão %s".formatted(
                data.month(), data.year(), data.creditCardName()
        );

        CreateTransactionDTO createTransactionDTO = new CreateTransactionDTO(
                data.accountId(),
                data.creditCardId(),
                null,
                description,
                data.totalPaid(),
                1,
                false,
                LocalDateTime.now(),
                TypeEnum.EXPENSE
        );

        Transaction transaction = transactionMapper.toEntity(createTransactionDTO);
        transaction.setAmount(data.totalPaid());
        transaction.setUserId(data.userId());
        transaction.setStatus(StatusTransactionEnum.PAID);
        transaction.setIdempotencyKey(idempotencyKey);

        transactionRepository.save(transaction);
        publishInvoicePaymentAfterCommit(transaction);
    }
}
