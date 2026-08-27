package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.CreditCardSelector;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardInternalService {

    private final CreditCardSelector cardSelector;

    public CreditCardSummaryDTO getCreditCardSummary(UUID id) {
        CreditCard creditCard = cardSelector.getCreditCardById(id);

        return new CreditCardSummaryDTO(
                creditCard.getId(),
                creditCard.getName(),
                creditCard.isDeleted()
        );
    }

    public CreditCardInternalSummaryDTO getCreditCardInternalSummary(UUID id) {
        CreditCard creditCard = cardSelector.getCreditCardById(id);

        return new CreditCardInternalSummaryDTO(
                creditCard.getId(),
                creditCard.getAccountId()
        );
    }
}
