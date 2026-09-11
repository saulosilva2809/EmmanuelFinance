package com.emmanuelfinance.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class InsufficientLimitOnCardException extends APIException {
        public InsufficientLimitOnCardException() {
            super(
                    HttpStatus.CONFLICT,
                    "Insufficient limit to restore the transaction."
            );
        }
    }
