package com.emmanuelfinance.category.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class CategoryAlreadyExists extends APIException {
    public CategoryAlreadyExists() {
        super(
                HttpStatus.CONFLICT,
                "There’s already a category with that name and type"
        );
    }
}
