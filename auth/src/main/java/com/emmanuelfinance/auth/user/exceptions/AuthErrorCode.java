package com.emmanuelfinance.auth.user.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode {
    PASSWORDS_DO_NOT_MATCH(
            HttpStatus.BAD_REQUEST,
            "The passwords don't match."
    ),
    USER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "There's already a user with that email."
    ),
    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "User not found."
    );

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}