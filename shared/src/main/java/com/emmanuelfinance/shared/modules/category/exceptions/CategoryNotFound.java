package com.emmanuelfinance.shared.modules.category.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import org.springframework.http.HttpStatus;

public class CategoryNotFound extends APIException {
    public CategoryNotFound() {
        super(
                HttpStatus.NOT_FOUND,
                "Category not found."
        );
    }
}
