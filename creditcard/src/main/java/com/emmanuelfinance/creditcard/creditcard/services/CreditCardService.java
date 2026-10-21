package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.*;
import com.emmanuelfinance.creditcard.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardService {

    private final AccountOwnershipValidator accountOwnershipValidator;
    private final SecurityUtils securityUtils;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardMapper cardMapper;
    private final CreditCardSelector cardSelector;
    private final CreditCardValidatorService creditCardValidatorService;
    private final CreditCardBalanceService creditCardBalanceService;
    private final OutboxService outboxService;

    @Transactional
    public ResponseCreditCardDTO create(CreateCreditCardDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        creditCardValidatorService.validateCreation(data);

        CreditCard creditCard = cardMapper.toEntity(data);

        creditCard.setUserId(userId);
        Optional.ofNullable(data.creditLimit())
                .ifPresent(creditLimit -> creditCard.setCreditLimit(creditLimit));
        Optional.ofNullable(data.creditLimit())
                .ifPresent(creditLimit -> creditCard.setAvailableLimit(creditLimit));

        CreditCard savedCreditCard = creditCardRepository.save(creditCard);
        outboxService.creditCardChanged(savedCreditCard);

        return cardMapper.toResponseDTO(savedCreditCard);
    }

    @Transactional
    public ResponseCreditCardDTO update(UUID cardId, UpdateCreditCardDTO data) {
        CreditCard creditCard = cardSelector.getCreditCardById(cardId);

        if (data.accountId() != null) {
            accountOwnershipValidator.validate(data.accountId());
        }

        creditCardBalanceService.updateAvailableLimit(creditCard, data);
        cardMapper.updateCreditCardFromDTO(data, creditCard);

        CreditCard updatedCard = creditCardRepository.saveAndFlush(creditCard);
        outboxService.creditCardChanged(updatedCard);

        return cardMapper.toResponseDTO(updatedCard);
    }

    @Transactional
    public void delete(UUID cardId) {
        CreditCard creditCard = cardSelector.getCreditCardById(cardId);

        creditCardRepository.delete(creditCard);
        outboxService.creditCardDeleted(creditCard);
    }

    @Transactional
    public void restore(UUID cardId) {
        CreditCard creditCard = cardSelector.getCreditCardByIdIncludingDeleted(cardId);

        creditCardValidatorService.validateRestoration(creditCard);

        creditCard.setDeleted(false);
        creditCardRepository.saveAndFlush(creditCard);
        outboxService.creditCardChanged(creditCard);
    }
}
