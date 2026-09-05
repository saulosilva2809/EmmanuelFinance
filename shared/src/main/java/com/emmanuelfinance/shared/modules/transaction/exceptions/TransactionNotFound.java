package com.emmanuelfinance.shared.modules.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class TransactionNotFound extends APIException {
    public TransactionNotFound() {
        super(
                HttpStatus.NOT_FOUND,
                "Transaction not found."
        );
    }
}
