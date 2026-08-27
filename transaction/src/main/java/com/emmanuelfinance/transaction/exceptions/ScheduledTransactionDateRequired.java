package com.emmanuelfinance.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class ScheduledTransactionDateRequired extends APIException {
    public ScheduledTransactionDateRequired() {
        super(
                HttpStatus.CONFLICT,
                "A date is required for scheduled transactions."
        );
    }
}
