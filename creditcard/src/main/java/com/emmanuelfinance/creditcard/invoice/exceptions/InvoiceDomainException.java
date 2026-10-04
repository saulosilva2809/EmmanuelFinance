package com.emmanuelfinance.creditcard.invoice.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import lombok.Getter;

@Getter
public class InvoiceDomainException extends APIException {

    private final InvoiceErrorCode errorCode;

    public InvoiceDomainException(InvoiceErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}