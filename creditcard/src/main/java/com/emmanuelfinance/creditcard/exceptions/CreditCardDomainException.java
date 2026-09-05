package com.emmanuelfinance.creditcard.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;

public class CreditCardDomainException extends APIException {

    public CreditCardDomainException(CreditCardErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
    }

    public CreditCardDomainException(CreditCardErrorCode errorCode, Object... args) {
        super(errorCode.getStatus(), String.format(errorCode.getMessage(), args));
    }
}