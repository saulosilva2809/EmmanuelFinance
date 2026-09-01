package com.emmanuelfinance.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class ScheduledTransactionDateInPastException extends APIException {
    public ScheduledTransactionDateInPastException() {
        super(
                HttpStatus.CONFLICT,
                "The scheduled date can't be in the past."
        );
    }
}
