package com.emmanuelfinance.account.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class RestoreAccountError extends APIException {
    public RestoreAccountError() {
        super(
                HttpStatus.CONFLICT,
                "It's not possible to restore an account that isn't deleted."
        );
    }
}
