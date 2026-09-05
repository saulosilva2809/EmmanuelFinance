package com.emmanuelfinance.transaction.services;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;
import com.emmanuelfinance.transaction.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionValidatorService {

    private final AccountOwnershipValidator accountOwnershipValidator;
    private final CategoryClientCacheService categoryClientCacheService;
    private final CreditCardClientCacheService creditCardClientCacheService;

    public final CreateValidations create = new CreateValidations();
    public final UpdateValidations update = new UpdateValidations();

    private void checkCategoryAndTransactionType(UUID categoryId, TypeEnum transactionType) {
        CategoryInternalSummaryDTO categoryDTO = categoryClientCacheService.getCategoryInternalSummaryDTO(categoryId);

        if (categoryDTO.type() != transactionType) {
            throw new CategoryTypeMismatch();
        }
    }

    private void validateScheduledDate(Boolean scheduled, LocalDateTime date) {
        boolean isScheduled = Boolean.TRUE.equals(scheduled);
        boolean hasDate = date != null;

        if (isScheduled) {
            if (!hasDate) {
                throw new ScheduledTransactionDateRequired();
            }

            if (date.isBefore(LocalDateTime.now().minusMinutes(1))) {
                throw new ScheduledTransactionDateInPastException();
            }
        } else {
            if (hasDate) {
                throw new UnscheduledTransactionDateNotAllowed();
            }
        }
    }

    public void validateCreditCardAccount(UUID accountId, UUID cardId) {
        if (cardId == null) {
            return;
        }

        CreditCardInternalSummaryDTO creditCard = creditCardClientCacheService.getCreditCardInternalSummaryDTO(cardId);

        if (creditCard == null || !creditCard.accountId().equals(accountId)) {
            throw new CreditCardNotFound();
        }
    }

    public void checkIfTransactionIsDeleted(Transaction transaction) {
        if (!transaction.isDeleted()) {
            throw new RestoreItemNotDeletedException();
        }
    }

    public class CreateValidations {
        public void validate(CreateTransactionDTO data) {
            if (data.creditCardId() == null && data.installmentsCount() != null && data.installmentsCount() > 1) {
                throw new TransactionDomainException(TransactionErrorCode.INSTALLMENTS_IN_TRANSACTION_ACCOUNT);
            }

            if (data.creditCardId() != null && !TypeEnum.EXPENSE.equals(data.type())) {
                throw new TransactionDomainException(TransactionErrorCode.CARD_TRANSACTION_TYPE);
            }

            if (data.installmentsCount() != null && data.installmentsCount() < 1) {
                throw new TransactionDomainException(TransactionErrorCode.NUMBER_OF_INSTALLMENTS);
            }

            accountOwnershipValidator.validate(data.accountId());
            validateCreditCardAccount(data.accountId(), data.creditCardId());
            checkCategoryAndTransactionType(data.categoryId(), data.type());
            validateScheduledDate(data.scheduled(), data.date());
        }
    }

    public class UpdateValidations {
        public void validate(Transaction existingTransaction, UpdateTransactionDTO data) {
            TypeEnum targetType = data.type() != null ? data.type() : existingTransaction.getType();

            if (data.accountId() != null && !data.accountId().equals(existingTransaction.getAccountId())) {
                accountOwnershipValidator.validate(data.accountId());
            }

            if (data.categoryId() != null) {
                checkCategoryAndTransactionType(data.categoryId(), targetType);
            }

            if (data.creditCardId() != null && !data.creditCardId().equals(existingTransaction.getCreditCardId())) {
                creditCardClientCacheService.getCreditCardInternalSummaryDTO(data.creditCardId());
            }

            if (!existingTransaction.isScheduled() && Boolean.TRUE.equals(data.scheduled())) {
                throw new CannotScheduleUnscheduledTransactionException();
            }

            if (Boolean.FALSE.equals(data.scheduled()) && data.date() != null) {
                throw new UnscheduledTransactionDateNotAllowed();
            } else if (Boolean.TRUE.equals(data.scheduled())) {
                validateScheduledDate(data.scheduled(), data.date());
            }
        }
    }
}