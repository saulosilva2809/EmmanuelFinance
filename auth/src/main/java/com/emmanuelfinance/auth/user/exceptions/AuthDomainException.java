package com.emmanuelfinance.auth.user.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import lombok.Getter;

@Getter
public class AuthDomainException extends APIException {

    private final AuthErrorCode errorCode;

    public AuthDomainException(AuthErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}