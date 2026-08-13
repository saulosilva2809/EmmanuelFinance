package com.emmanuelfinance.shared.modules.account.exceptions;

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
