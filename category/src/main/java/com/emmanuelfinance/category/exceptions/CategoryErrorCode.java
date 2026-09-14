package com.emmanuelfinance.category.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CategoryErrorCode {
    CATEGORY_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "There’s already a category with that name and type."
    ),
    RESTORE_CATEGORY_NOT_DELETED(
            HttpStatus.CONFLICT,
            "It's not possible to restore a category that isn't deleted."
    );

    private final HttpStatus status;
    private final String message;

    CategoryErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}