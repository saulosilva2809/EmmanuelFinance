package com.emmanuelfinance.creditcard.payment.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import lombok.Getter;

@Getter
public class PaymentDomainException extends APIException {

    private final PaymentErrorCode errorCode;

    public PaymentDomainException(PaymentErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}