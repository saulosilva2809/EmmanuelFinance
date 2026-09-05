package com.emmanuelfinance.shared.modules.transaction;

import com.emmanuelfinance.shared.modules.transaction.exceptions.TransactionNotFound;
import feign.Response;
import feign.codec.ErrorDecoder;

public class TransactionErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new TransactionNotFound();
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}