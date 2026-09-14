package com.emmanuelfinance.category.exceptions;

import com.emmanuelfinance.config.exceptions.APIException;
import lombok.Getter;

@Getter
public class CategoryDomainException extends APIException {

    private final CategoryErrorCode errorCode;

    public CategoryDomainException(CategoryErrorCode errorCode) {
        super(errorCode.getStatus(), errorCode.getMessage());
        this.errorCode = errorCode;
    }
}