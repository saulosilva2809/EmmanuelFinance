package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreditCardSelector {

    private final CreditCardRepository creditCardRepository;
    private final SecurityUtils securityUtils;

    public CreditCard getCreditCardById(UUID cardId) {
        UUID userId = securityUtils.getCurrentUserId();
        CreditCard creditCard = creditCardRepository.findByIdAndUserIdAndDeletedFalse(cardId, userId)
                .orElseThrow(() -> new CreditCardNotFound());

        return creditCard;
    }

    public CreditCard getCreditCardByIdIncludingDeleted(UUID cardId) {
        UUID userId = securityUtils.getCurrentUserId();
        CreditCard creditCard = creditCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CreditCardNotFound());

        return creditCard;
    }
}
