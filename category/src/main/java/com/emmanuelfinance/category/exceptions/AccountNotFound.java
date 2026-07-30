package com.emmanuelfinance.category.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class AccountNotFound extends APIException {
    public AccountNotFound() {
        super(
                HttpStatus.NOT_FOUND,
                "Account not found."
        );
    }
}
