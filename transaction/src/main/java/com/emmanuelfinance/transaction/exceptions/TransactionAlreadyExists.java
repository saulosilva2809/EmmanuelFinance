package com.emmanuelfinance.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class TransactionAlreadyExists extends APIException {
    public TransactionAlreadyExists() {
        super(
                HttpStatus.CONFLICT,
                "This transaction has already been made."
        );
    }
}
