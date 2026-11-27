package com.emmanuelfinance.transaction.transfer.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;

public class TransferDomainException extends APIException {

    public TransferDomainException(TransferErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
    }
}