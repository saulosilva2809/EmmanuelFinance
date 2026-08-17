package com.emmanuelfinance.creditcard.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class CheckCardAndAccountBankError extends APIException {
    public CheckCardAndAccountBankError() {
        super(
                HttpStatus.CONFLICT,
                "The bank for the credit card and the account should be the same."
        );
    }
}
