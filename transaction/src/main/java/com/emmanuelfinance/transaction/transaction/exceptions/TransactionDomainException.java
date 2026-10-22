package com.emmanuelfinance.transaction.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;

public class TransactionDomainException extends APIException {

    public TransactionDomainException(TransactionErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
    }
}