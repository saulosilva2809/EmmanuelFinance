package com.emmanuelfinance.auth.user.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class PasswordsDoNotMatch extends APIException {
    public PasswordsDoNotMatch() {
        super(
                HttpStatus.BAD_REQUEST,
                "The passwords don't match."
        );
    }
}
