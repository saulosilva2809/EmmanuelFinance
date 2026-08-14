package com.emmanuelfinance.account.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class RestoreAccount extends APIException {
    public RestoreAccount() {
        super(
                HttpStatus.CONFLICT,
                "It's not possible to restore an account that isn't deleted."
        );
    }
}
