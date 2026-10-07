package com.emmanuelfinance.creditcard.payment.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PaymentErrorCode {
    INSUFFICIENT_BALANCE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Insufficient balance in the account provided."
    ),
    INVOICE_ALREADY_PAID(
            HttpStatus.CONFLICT,
            "This invoice has already been paid."
    ),
    INVOICE_NOTHING_TO_PAY(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "This invoice has no amount to be paid."
    );

    private final HttpStatus status;
    private final String message;

    PaymentErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
