package com.emmanuelfinance.creditcard.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class CreditCardNotFound extends APIException {
    public CreditCardNotFound() {
        super(
                HttpStatus.NOT_FOUND,
                "Credit card not found."
        );
    }
}
