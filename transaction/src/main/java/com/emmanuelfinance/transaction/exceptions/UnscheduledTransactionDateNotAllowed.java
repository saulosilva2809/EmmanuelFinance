package com.emmanuelfinance.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class UnscheduledTransactionDateNotAllowed extends APIException {
    public UnscheduledTransactionDateNotAllowed() {
        super(
                HttpStatus.CONFLICT,
                "Date cannot be provided for non-scheduled transactions."
        );
    }
}
