package com.emmanuelfinance.auth.user.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class UserAlreadyExists extends APIException {
    public UserAlreadyExists() {
        super(
                HttpStatus.CONFLICT,
                "There's already a user with that email."
        );
    }
}
