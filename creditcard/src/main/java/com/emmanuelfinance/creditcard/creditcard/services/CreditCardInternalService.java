package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardInternalSummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Serviço das chamadas ENTRE serviços. Lê do Postgres (fonte da verdade), nunca do Mongo: o Mongo recebe
// os dados pelo Outbox e pode estar atrasado, e quem chama aqui usa o limite disponível para tomar decisões.
@Service
@RequiredArgsConstructor
public class CreditCardInternalService {

    private final CreditCardSelector cardSelector;
    private final CreditCardRepository creditCardRepository;
    private final OutboxService outboxService;

    public CreditCardSummaryDTO getCreditCardSummary(UUID creditCardId) {
        CreditCard creditCard = cardSelector.getCreditCardById(creditCardId);

        return new CreditCardSummaryDTO(
                creditCard.getId(),
                creditCard.getName(),
                creditCard.isDeleted()
        );
    }

    public CreditCardInternalSummaryDTO getCreditCardInternalSummary(UUID creditCardId) {
        CreditCard creditCard = cardSelector.getCreditCardById(creditCardId);

        return new CreditCardInternalSummaryDTO(
                creditCard.getId(),
                creditCard.getAccountId(),
                creditCard.getAvailableLimit(),
                creditCard.getDueDay(),
                creditCard.getClosingDay()
        );
    }

    // @Transactional: o Outbox exige uma transação, e assim todos os cartões da conta e as suas linhas confirmam juntos
    @Transactional
    public void deactivateCardsByAccountId(UUID accountId) {
        List<CreditCard> cards = cardSelector.findByAccountId(accountId);
        cards.forEach(card -> {
            creditCardRepository.delete(card);
            outboxService.creditCardDeleted(card);
        });
    }
}
