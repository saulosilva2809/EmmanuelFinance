package com.emmanuelfinance.creditcard.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class RestoreCreditCardError extends APIException {
    public RestoreCreditCardError() {
        super(
                HttpStatus.CONFLICT,
                "It's not possible to restore an credit card that isn't deleted."
        );
    }
}
