package com.emmanuelfinance.account.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AccountErrorCode {
    ACCOUNT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Account not found."
    ),
    RESTORE_ACCOUNT_NOT_DELETED(
            HttpStatus.CONFLICT,
            "It's not possible to restore an account that isn't deleted."
    );

    private final HttpStatus status;
    private final String message;

    AccountErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}