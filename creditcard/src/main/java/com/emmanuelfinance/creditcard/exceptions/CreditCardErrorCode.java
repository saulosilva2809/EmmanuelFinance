package com.emmanuelfinance.creditcard.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CreditCardErrorCode {
    INSUFFICIENT_LIMIT(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Insufficient limit to restore the transaction."
    ),
    BANK_OF_CARD_AND_ACCOUNT_DIFFERENT(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "The bank for the credit card and the account should be the same."
    ),
    RESTORE_CARD_WITH_DELETED_ACCOUNT(
            HttpStatus.CONFLICT,
            "It's not possible to restore the card whose account is deleted."
    ),
    RESTORE_CARD_NOT_DELETED(
            HttpStatus.CONFLICT,
            "It's not possible to restore an credit card that isn't deleted."
    );

    private final HttpStatus status;
    private final String message;

    CreditCardErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}