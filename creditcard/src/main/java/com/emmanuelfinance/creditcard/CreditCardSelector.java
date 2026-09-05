package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.swing.undo.CannotRedoException;
import java.util.List;
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

    public CreditCard getCreditCardByIdInternal(UUID cardId) {
        CreditCard creditCard = creditCardRepository.findById(cardId)
                .orElseThrow(CannotRedoException::new);

        return creditCard;
    }

    public CreditCard getCreditCardByIdIncludingDeleted(UUID cardId) {
        UUID userId = securityUtils.getCurrentUserId();
        CreditCard creditCard = creditCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CreditCardNotFound());

        return creditCard;
    }

    public List<CreditCard> findByAccountId(UUID accountId) {
        List<CreditCard> cards = creditCardRepository.findByAccountId(
                accountId
        );

        return cards;
    }
}
