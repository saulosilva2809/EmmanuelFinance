package com.emmanuelfinance.transaction.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TransactionErrorCode {
    INSTALLMENTS_IN_TRANSACTION_ACCOUNT(
            HttpStatus.CONFLICT,
            "Account transactions can't be split into installments"
    ),
    NUMBER_OF_INSTALLMENTS(
            HttpStatus.CONFLICT,
            "The number of installments must be at least 1."
    ),
    CARD_TRANSACTION_TYPE(
            HttpStatus.CONFLICT,
            "Credit card purchases should be the Expense type."
    );

    private final HttpStatus status;
    private final String message;

    TransactionErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}