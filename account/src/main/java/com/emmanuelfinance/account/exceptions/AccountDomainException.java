package com.emmanuelfinance.account.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import lombok.Getter;

@Getter
public class AccountDomainException extends APIException {

    private final AccountErrorCode errorCode;

    public AccountDomainException(AccountErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}