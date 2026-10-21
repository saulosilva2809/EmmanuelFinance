package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreditCardDocumentSelector {

    private final CreditCardDocumentRepository creditCardDocumentRepository;
    private final SecurityUtils securityUtils;

    public CreditCardDocument getByIdAndUserIdAndDeletedFalse(UUID accountId) {
        UUID userId = securityUtils.getCurrentUserId();

        CreditCardDocument creditCard = creditCardDocumentRepository.findByIdAndUserIdAndDeletedFalse(
                accountId,
                userId
        ).orElseThrow(CreditCardNotFound::new);

        return creditCard;
    }
}
