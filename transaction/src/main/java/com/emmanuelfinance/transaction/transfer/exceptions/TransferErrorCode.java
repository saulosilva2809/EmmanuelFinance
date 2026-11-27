package com.emmanuelfinance.transaction.transfer.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TransferErrorCode {
    SAME_ACCOUNTS(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "The accounts entered can't be the same."
    ),
    ACCOUNT_OWNERSHIP_EXCEPTION(
            HttpStatus.UNPROCESSABLE_ENTITY,
        "You can only make transfers between accounts that you own."
    ),
    VALUE_MUST_BE_POSITIVE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "The transfer amount must be greater than 0."
    ),
    INSUFFICIENT_BALANCE(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Insufficient balance in the account."
    ),
    FAILED_TRANSFER_CANNOT_BE_CHANGED(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "A failed transfer can't be changed."
    ),
    RESTORE_TRANSFER_NOT_DELETED(
            HttpStatus.CONFLICT,
            "It's not possible to restore a transfer that isn't deleted."
    ),
    TRANSFER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Transfer not found."
    );

    private final HttpStatus status;
    private final String message;

    TransferErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}