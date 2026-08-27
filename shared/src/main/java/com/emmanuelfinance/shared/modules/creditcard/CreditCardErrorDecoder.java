package com.emmanuelfinance.shared.modules.creditcard;

import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import feign.Response;
import feign.codec.ErrorDecoder;

public class CreditCardErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new CreditCardNotFound();
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}