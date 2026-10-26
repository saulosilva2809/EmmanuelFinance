package com.emmanuelfinance.transaction.transaction.services;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategoryInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.transaction.dtos.UpdateTransactionDTO;
import com.emmanuelfinance.transaction.transaction.exceptions.TransactionDomainException;
import com.emmanuelfinance.transaction.transaction.exceptions.TransactionErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionValidatorService {

    private final AccountOwnershipValidator accountOwnershipValidator;
    private final CategoryClientCacheService categoryClientCacheService;
    private final CreditCardClientCacheService creditCardClientCacheService;
    private final AccountClientCacheService accountClientCacheService;

    public final CreateValidations create = new CreateValidations();
    public final UpdateValidations update = new UpdateValidations();
    public final RestoreValidations restore = new RestoreValidations();

    // valida se o tipo da categoria é o mesmo tipo da transação (INCOME, EXPENSE)
    private void checkCategoryAndTransactionType(UUID categoryId, TypeTransactionEnum transactionType) {
        CategoryInternalSummaryDTO categoryDTO = categoryClientCacheService.getCategoryInternalSummaryDTO(categoryId);

        if (categoryDTO.type() != transactionType) {
            throw new TransactionDomainException(TransactionErrorCode.INCOMPATIBLE_CATEGORY_TYPE);
        }
    }

    // validações de data e agendamento
    private void validateScheduledDate(Boolean scheduled, LocalDateTime date) {
        boolean isScheduled = Boolean.TRUE.equals(scheduled);
        boolean hasDate = date != null;

        // es está agendado
        if (isScheduled) {
            // se não tem data
            if (!hasDate) {
                throw new TransactionDomainException(TransactionErrorCode.SCHEDULED_TRANSACTION_DATE_REQUIRED);
            }

            // se foi agendado no passado
            if (date.isBefore(LocalDateTime.now().minusMinutes(1))) {
                throw new TransactionDomainException(TransactionErrorCode.TRANSACTION_SCHEDULED_IN_THE_PAST);
            }

        // se não está agendado
        } else {
            // se tem data
            if (hasDate) {
                throw new TransactionDomainException(TransactionErrorCode.UNSCHEDULED_TRANSACTION_DATE_NOT_ALLOWED);
            }
        }
    }

    // valida se a conta do cartão de crédito é a mesma conta da transação
    public void validateCreditCardAccount(UUID accountId, UUID cardId) {
        if (cardId == null) {
            return;
        }

        CreditCardInternalSummaryDTO creditCard = creditCardClientCacheService.getCreditCardInternalSummaryDTO(cardId);

        if (creditCard == null || !creditCard.accountId().equals(accountId)) {
            throw new CreditCardNotFound();
        }
    }

    // valida se tem limite disponível o cartão de crédito
    public void validateCreditCardLimit(UUID creditCardId, BigDecimal amount) {
        if (creditCardId == null) {
            return;
        }

        CreditCardInternalSummaryDTO creditCard = creditCardClientCacheService.getCreditCardInternalSummaryDTO(creditCardId);

        if (amount.compareTo(creditCard.availableLimit()) > 0) {
            throw new TransactionDomainException(TransactionErrorCode.INSUFFICIENT_LIMIT_ON_THE_CARD);
        }
    }

    // verifica se a transação está deletada
    public void checkIfTransactionIsDeleted(Transaction transaction) {
        if (!transaction.isDeleted()) {
            throw new TransactionDomainException(TransactionErrorCode.RESTORE_TRANSACTION_NOT_DELETED);
        }
    }

    // valida se é uma EXPENSE e tem saldo na conta
    public void validateAccountBalance(BigDecimal value, UUID accountId, TypeTransactionEnum type) {
        AccountSummaryInternalDTO accountSummary = accountClientCacheService.getInternalAccountById(accountId);

        if (value.compareTo(accountSummary.currentBalance()) > 0 && TypeTransactionEnum.EXPENSE.equals(type)) {
            throw new TransactionDomainException(TransactionErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    // validações agrupadas para o create
    public class CreateValidations {
        public void validate(CreateTransactionDTO data) {

            // se o cartão de crédito é nulo
            if (data.creditCardId() == null) {
                // sem cartão não existe parcelamento
                if (data.installmentsCount() != null && data.installmentsCount() > 1) {
                    throw new TransactionDomainException(TransactionErrorCode.INSTALLMENTS_IN_TRANSACTION_ACCOUNT);
                }
            // se o cartão de crédito não é nulo
            } else {
                // se é um INCOME
                if (!TypeTransactionEnum.EXPENSE.equals(data.type())) {
                    throw new TransactionDomainException(TransactionErrorCode.CARD_TRANSACTION_TYPE);
                }

                // valida o limite
                validateCreditCardLimit(data.creditCardId(), data.amount());
            }

            // parcela menor que 1 não existe, com ou sem cartão
            if (data.installmentsCount() != null && data.installmentsCount() < 1) {
                throw new TransactionDomainException(TransactionErrorCode.NUMBER_OF_INSTALLMENTS);
            }

            // valida o dono da conta antes de qualquer consulta ao saldo
            accountOwnershipValidator.validate(data.accountId());

            // sem cartão, valida se a conta tem saldo
            if (data.creditCardId() == null) {
                validateAccountBalance(data.amount(), data.accountId(), data.type());
            }

            // valida se a conta do cartão de crédito é a mesma conta da transação
            validateCreditCardAccount(data.accountId(), data.creditCardId());

            // valida se o tipo da categoria é o mesmo tipo da transação (INCOME, EXPENSE)
            checkCategoryAndTransactionType(data.categoryId(), data.type());

            // validações de data e agendamento
            validateScheduledDate(data.scheduled(), data.date());
        }
    }

    // validações agrupadas para o update
    public class UpdateValidations {
        public void validate(Transaction existingTransaction, UpdateTransactionDTO data) {
            // se um novo tipo foi imformado, pega ele, se não pega o da transação existente
            TypeTransactionEnum targetType = data.type() != null ? data.type() : existingTransaction.getType();

            // se foi informada uma nova conta e é diferente da conta cadastrada na transação existente
            if (data.accountId() != null && !data.accountId().equals(existingTransaction.getAccountId())) {
                accountOwnershipValidator.validate(data.accountId());
            }

            // se a categoria não é nula
            if (data.categoryId() != null) {
                checkCategoryAndTransactionType(data.categoryId(), targetType);
            }

            // se a trasação existente não estava agendada e foi marcada como agendada
            if (!existingTransaction.isScheduled() && Boolean.TRUE.equals(data.scheduled())) {
                throw new TransactionDomainException(TransactionErrorCode.CANNOT_SCHEDULE_UNSCHEDULED_TRANSACTION);
            }

            // se não esta agendada e uma data foi informada
            if (Boolean.FALSE.equals(data.scheduled()) && data.date() != null) {
                throw new TransactionDomainException(TransactionErrorCode.UNSCHEDULED_TRANSACTION_DATE_NOT_ALLOWED);
            }

            // se está agendada
            boolean isScheduled = data.scheduled() != null ? data.scheduled() : existingTransaction.isScheduled();

            // se está agendada e foi informada uma data
            if (Boolean.TRUE.equals(isScheduled) && data.date() != null) {
                // faz as validações de agendamento
                validateScheduledDate(isScheduled, data.date());
            }

            // se tem valor e não é no cartão de crédito
            if (data.amount() != null && existingTransaction.getCreditCardId() == null) {
                // pega a conta nova (se tiver mudado)
                UUID accountId = data.accountId() != null ? data.accountId() : existingTransaction.getAccountId();

                // valida o saldo
                validateAccountBalance(data.amount(), accountId, targetType);
            }
        }
    }

    // validações agrupadas para o restore
    public class RestoreValidations {
        public void validate(Transaction transaction) {
            // se a transação está deletada
            checkIfTransactionIsDeleted(transaction);

            // se tem um cartão de crédito
            if (transaction.getCreditCardId() != null) {
                // verifica se tem limite no cartão
                // se for uma receita -> deve subtrair do saldo após excluir
                // se for uma despesa -> deve adicionar ao saldo após excluir
                validateCreditCardLimit(transaction.getCreditCardId(), transaction.getAmount());
            }
        }
    }
}