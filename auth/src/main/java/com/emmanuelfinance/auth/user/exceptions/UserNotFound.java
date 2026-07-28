package com.emmanuelfinance.auth.user.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class UserNotFound extends APIException {
    public UserNotFound() {
        super(
                HttpStatus.NOT_FOUND,
                "User not found."
        );
    }
}
