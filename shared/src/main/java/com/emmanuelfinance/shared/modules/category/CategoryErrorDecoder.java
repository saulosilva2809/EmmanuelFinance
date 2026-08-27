package com.emmanuelfinance.shared.modules.category;

import com.emmanuelfinance.shared.modules.category.exceptions.CategoryNotFound;
import feign.Response;
import feign.codec.ErrorDecoder;

public class CategoryErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new CategoryNotFound();
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}