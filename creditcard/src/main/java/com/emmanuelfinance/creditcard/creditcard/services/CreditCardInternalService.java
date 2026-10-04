package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardInternalService {

    private final CreditCardSelector cardSelector;
    private final CreditCardRepository creditCardRepository;

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
                creditCard.getAccountId(),
                creditCard.getAvailableLimit(),
                creditCard.getDueDay(),
                creditCard.getClosingDay()
        );
    }

    public void deactivateCardsByAccountId(UUID accountId) {
        List<CreditCard> cards = cardSelector.findByAccountId(accountId);
        cards.forEach(card -> {
            creditCardRepository.delete(card);
        });
    }
}
