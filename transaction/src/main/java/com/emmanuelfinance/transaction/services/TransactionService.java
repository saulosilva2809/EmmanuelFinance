package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.annotation.WithDeletedFilter;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.*;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.dtos.TransactionFiltersDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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

    private void setTransactionStatus(Transaction transaction) {
        if (Boolean.TRUE.equals(transaction.isScheduled())) {
            transaction.setStatus(StatusTransactionEnum.PENDING);
        } else {
            transaction.setStatus(StatusTransactionEnum.PAID);
        }
    }

    @Transactional
    public ResponseTransactionDTO create(CreateTransactionDTO data) {
        // TODO: nova regra de negócio, garantir que a account seja o mesmo do credit card

        UUID userId = securityUtils.getCurrentUserId();

        transactionValidatorService.create.validate(data);

        String idempotencyKey = idempotencyService.generateIdempotencyKey(userId, data);
        idempotencyService.validateAndLock(idempotencyKey);

        Transaction transaction = transactionMapper.toEntity(data);
        transaction.setUserId(userId);
        transaction.setIdempotencyKey(idempotencyKey);
        setTransactionStatus(transaction);

        transactionValidatorService.validateCreditCardAccount(transaction.getAccountId(), transaction.getCreditCardId());

        Transaction savedTransaction = transactionRepository.save(transaction);
        transactionEventsService.publishTransactionCreatedEvent(transaction);

        // TODO: tratar quando a compra for feita no cartão

        return transactionMapper.toResponseDTO(savedTransaction);
    }

    public ResponseTransactionDTO view(UUID id) {
        Transaction transaction = transactionSelector.getTransactionById(id);
        return transactionMapper.toResponseDTO(transaction);
    }

    @WithDeletedFilter()
    public PageResponseDTO<ResponseTransactionDTO> list(TransactionFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Transaction> specification = TransactionSpecification.withFilter(
                filters,
                userId,
                false
        );
        Page<Transaction> page = transactionRepository.findAll(
                specification,
                pageable
        );
        Page<ResponseTransactionDTO> dtoPage = page.map(transactionMapper::toResponseDTO);

        return PageResponseDTO.from(dtoPage);
    }

    @WithDeletedFilter(enabled = false)
    public PageResponseDTO<ResponseTransactionDTO> listDeleted(TransactionFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Transaction> specification = TransactionSpecification.withFilter(
                filters,
                userId,
                true
        );
        Page<Transaction> page = transactionRepository.findAll(
                specification,
                pageable
        );
        Page<ResponseTransactionDTO> dtoPage = page.map(transactionMapper::toResponseDTO);

        return PageResponseDTO.from(dtoPage);
    }

    @Transactional
    public ResponseTransactionDTO update(UUID transactionId, UpdateTransactionDTO data) {
        Transaction transaction = transactionSelector.getTransactionById(transactionId);

        UUID oldAccountId = transaction.getAccountId();
        BigDecimal oldAmount = transaction.getAmount();
        TypeEnum oldType = transaction.getType();

        transactionValidatorService.update.validate(transaction, data);

        transactionMapper.updateEntityFromDTO(data, transaction);
        transactionValidatorService.validateCreditCardAccount(transaction.getAccountId(), transaction.getCreditCardId());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        transactionEventsService.publishTransactionUpdatedEvent(updatedTransaction, oldAccountId, oldAmount, oldType);

        return transactionMapper.toResponseDTO(updatedTransaction);
    }

    @Transactional
    public void delete(UUID transactionId) {
        Transaction transaction = transactionSelector.getTransactionById(transactionId);
        transactionRepository.delete(transaction);

        transactionEventsService.publishTransactionDeletedEvent(transaction);
    }

    @Transactional
    public void restore(UUID transactionId) {
        Transaction transaction = transactionSelector.getTransactionByIdIncluingDeleted(transactionId);
        transactionValidatorService.checkIfTransactionIsDeleted(transaction);

        transaction.setDeleted(false);
        transactionRepository.save(transaction);

        transactionEventsService.publishTransactionRestoreEvent(transaction);
    }
}
