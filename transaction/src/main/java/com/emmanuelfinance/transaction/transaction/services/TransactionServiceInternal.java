package com.emmanuelfinance.transaction.transaction.services;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.transaction.transaction.dtos.TransactionSummaryDTO;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.TransactionMapper;
import com.emmanuelfinance.transaction.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.transaction.dtos.IdempotencyDataDTO;
import com.emmanuelfinance.transaction.transaction.outbox.TransactionOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TransactionOutboxEventsService transactionOutboxEventsService;
    private final IdempotencyService idempotencyService;
    private final TransactionOutbox transactionOutbox;

    public TransactionSummaryDTO getSummaryDTO(UUID transactionId) {
        // só o dono recebe o resumo
        Transaction transaction = transactionSelector.getTransactionById(transactionId);

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

    @Transactional
    public void createTransactionRelatedToInvoicePayment(InvoicePaymentDTO data) {
        String idempotencyKey = idempotencyService.generateKey(IdempotencyDataDTO.builder()
                .userId(data.userId())
                .referenceId(data.paymentId())
                .build());

        // o kafka pode entregar o mesmo pagamento duas vezes: a chave da fatura mostra se ele já foi processado
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
                TypeTransactionEnum.EXPENSE
        );

        Transaction transaction = transactionMapper.toEntity(createTransactionDTO);
        transaction.setAmount(data.totalPaid());
        transaction.setUserId(data.userId());
        // guarda a fatura para poder desfazer o pagamento se a conta não for debitada
        transaction.setInvoiceId(data.invoiceId());
        transaction.setStatus(StatusTransactionEnum.PAID);
        transaction.setIdempotencyKey(idempotencyKey);

        transactionRepository.save(transaction);
        transactionOutbox.transactionChanged(transaction);
        transactionOutboxEventsService.publishInvoicePaymentCreatedEventIfPaid(transaction);
    }
}
