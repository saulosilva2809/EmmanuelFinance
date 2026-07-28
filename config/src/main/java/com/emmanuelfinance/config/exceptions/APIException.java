package com.emmanuelfinance.config.exceptions;

import org.springframework.http.HttpStatus;

public abstract class APIException extends RuntimeException {

    private final HttpStatus status;

    protected APIException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return this.status;
    }

}
