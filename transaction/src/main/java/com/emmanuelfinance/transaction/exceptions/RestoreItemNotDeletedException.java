package com.emmanuelfinance.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class RestoreItemNotDeletedException extends APIException {
    public RestoreItemNotDeletedException() {
        super(
                HttpStatus.CONFLICT,
                "It's not possible to restore an transaction that isn't deleted."
        );
    }
}
