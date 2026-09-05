package com.emmanuelfinance.creditcard.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CreditCardErrorCode {
    INSUFFICIENT_LIMIT(
            HttpStatus.CONFLICT,
            "Insufficient limit to restore the transaction."
    );

    private final HttpStatus status;
    private final String message;

    CreditCardErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}