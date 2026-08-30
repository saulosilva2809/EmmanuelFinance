package com.emmanuelfinance.shared.modules.account;

import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import feign.Response;
import feign.codec.ErrorDecoder;

public class AccountErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new AccountNotFound();
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}