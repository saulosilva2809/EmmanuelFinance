package com.emmanuelfinance.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class CannotScheduleUnscheduledTransactionException extends APIException {
    public CannotScheduleUnscheduledTransactionException() {
        super(
                HttpStatus.CONFLICT,
                "You can't schedule a transaction that has already been made."
        );
    }
}
