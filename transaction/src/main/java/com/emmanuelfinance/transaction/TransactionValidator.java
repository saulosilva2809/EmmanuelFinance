package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;
import com.emmanuelfinance.transaction.exceptions.CategoryTypeMismatch;
import com.emmanuelfinance.transaction.exceptions.ScheduledTransactionDateRequired;
import com.emmanuelfinance.transaction.exceptions.UnscheduledTransactionDateNotAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionValidator {

    private final AccountOwnershipValidator accountOwnershipValidator;
    private final CategoryClientCacheService categoryClientCacheService;
    private final CreditCardClientCacheService creditCardClientCacheService;

    public final CreateValidations create = new CreateValidations();
    public final UpdateValidations update = new UpdateValidations();

    public void validateCreditCardAccount(UUID accountId, UUID cardId) {
        if (cardId == null) {
            return;
        }

        CreditCardInternalSummaryDTO creditCard = creditCardClientCacheService.getCreditCardInternalSummaryDTO(cardId);

        if (creditCard == null || !creditCard.accountId().equals(accountId)) {
            throw new CreditCardNotFound();
        }
    }

    private void checkCategoryAndTransactionType(UUID categoryId, TypeEnum transactionType) {
        CategoryInternalSummaryDTO categoryDTO = categoryClientCacheService.getCategoryInternalSummaryDTO(categoryId);

        if (categoryDTO.type() != transactionType) {
            throw new CategoryTypeMismatch();
        }
    }

    private void validateScheduledDate(Boolean scheduled, LocalDateTime date) {
        boolean isScheduled = Boolean.TRUE.equals(scheduled);
        boolean hasDate = date != null;

        if (isScheduled && !hasDate) {
            throw new ScheduledTransactionDateRequired();
        } else if (!isScheduled && hasDate) {
            throw new UnscheduledTransactionDateNotAllowed();
        }
    }

    public class CreateValidations {
        public void validate(CreateTransactionDTO data) {
            accountOwnershipValidator.validate(data.accountId());
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

            if (Boolean.FALSE.equals(data.scheduled()) && data.date() != null) {
                throw new UnscheduledTransactionDateNotAllowed();
            } else if (Boolean.TRUE.equals(data.scheduled())) {
                validateScheduledDate(data.scheduled(), data.date());
            }
        }
    }
}