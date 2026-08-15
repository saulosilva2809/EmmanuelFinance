package com.emmanuelfinance.category.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class RestoreCategoryError extends APIException {
    public RestoreCategoryError() {
        super(
                HttpStatus.CONFLICT,
                "It's not possible to restore an category that isn't deleted."
        );
    }
}
