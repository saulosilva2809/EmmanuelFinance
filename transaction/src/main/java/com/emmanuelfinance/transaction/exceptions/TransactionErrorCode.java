package com.emmanuelfinance.transaction.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TransactionErrorCode {
    // transactions
    INSTALLMENTS_IN_TRANSACTION_ACCOUNT(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Account transactions can't be split into installments"
    ),
    NUMBER_OF_INSTALLMENTS(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "The number of installments must be at least 1."
    ),
    RESTORE_TRANSACTION_NOT_DELETED(
            HttpStatus.CONFLICT,
            "It's not possible to restore an transaction that isn't deleted."
    ),
    TRANSACTION_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "This transaction has already been made."
    ),

    // schedule
    CANNOT_SCHEDULE_UNSCHEDULED_TRANSACTION(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "You can't schedule a transaction that has already been made."
    ),
    TRANSACTION_SCHEDULED_IN_THE_PAST(
            HttpStatus.BAD_REQUEST,
            "The scheduled date can't be in the past."
    ),
    SCHEDULED_TRANSACTION_DATE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "A date is required for scheduled transactions."
    ),
    UNSCHEDULED_TRANSACTION_DATE_NOT_ALLOWED(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Date cannot be provided for non-scheduled transactions."
    ),

    // card
    CARD_TRANSACTION_TYPE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Credit card purchases should be the Expense type."
    ),
    CARD_TRANSACTIONS_CANNOT_BE_CHANGED(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "You can't update a transaction made with the card."
    ),
    INSUFFICIENT_LIMIT_ON_THE_CARD(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Insufficient limit to restore the transaction."
    ),

    // category
    INCOMPATIBLE_CATEGORY_TYPE(
            HttpStatus.CONFLICT,
            "Category type does not match transaction type."
    ),

    // account
    INSUFFICIENT_BALANCE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Insufficient balance in the selected account."
    );


    private final HttpStatus status;
    private final String message;

    TransactionErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}