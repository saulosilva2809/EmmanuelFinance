package com.emmanuelfinance.creditcard.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class RestoreItemNotDeletedException extends APIException {
    public RestoreItemNotDeletedException() {
        super(
                HttpStatus.CONFLICT,
                "It's not possible to restore an credit card that isn't deleted."
        );
    }
}
