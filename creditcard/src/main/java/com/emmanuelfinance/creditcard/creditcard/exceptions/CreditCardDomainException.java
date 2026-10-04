package com.emmanuelfinance.creditcard.creditcard.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import lombok.Getter;

@Getter
public class CreditCardDomainException extends APIException {

    private final CreditCardErrorCode errorCode;

    public CreditCardDomainException(CreditCardErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}