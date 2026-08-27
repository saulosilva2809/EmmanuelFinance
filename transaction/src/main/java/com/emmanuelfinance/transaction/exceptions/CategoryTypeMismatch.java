package com.emmanuelfinance.transaction.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class CategoryTypeMismatch extends APIException {
    public CategoryTypeMismatch() {
        super(
                HttpStatus.CONFLICT,
                "Category type does not match transaction type."
        );
    }
}
