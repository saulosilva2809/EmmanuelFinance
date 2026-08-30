package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.*;
import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.shared.annotation.WithDeletedFilter;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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

    public ResponseCreditCardDTO create(CreateCreditCardDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        creditCardValidatorService.validateCreation(data);

        CreditCard creditCard = cardMapper.toEntity(data);

        creditCard.setUserId(userId);
        Optional.ofNullable(data.creditLimit())
                .ifPresent(creditLimit -> creditCard.setCreditLimit(creditLimit));

        CreditCard savedCreditCard = creditCardRepository.save(creditCard);

        return cardMapper.toResponseDTO(savedCreditCard);
    }

    public ResponseCreditCardDTO view(UUID cardId) {
        CreditCard creditCard = cardSelector.getCreditCardById(cardId);
        return cardMapper.toResponseDTO(creditCard);
    }

    @WithDeletedFilter()
    public PageResponseDTO<ResponseCreditCardDTO> list(CreditCardFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<CreditCard> specification = CreditCardSpecification.withFilter(filters, userId, false);
        Page<CreditCard> page = creditCardRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseCreditCardDTO> dtoPage = page.map(cardMapper::toResponseDTO);

        return PageResponseDTO.from(dtoPage);
    }

    @WithDeletedFilter(enabled = false)
    public PageResponseDTO<ResponseCreditCardDTO> listDeleted(CreditCardFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<CreditCard> specification = CreditCardSpecification.withFilter(filters, userId, true);
        Page<CreditCard> page = creditCardRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseCreditCardDTO> dtoPage = page.map(cardMapper::toResponseDTO);

        return PageResponseDTO.from(dtoPage);
    }

    public ResponseCreditCardDTO update(UUID cardId, UpdateCreditCardDTO data) {
        CreditCard creditCard = cardSelector.getCreditCardById(cardId);

        if (data.accountId() != null) {
            accountOwnershipValidator.validate(data.accountId());
        }

        cardMapper.updateCreditCardFromDTO(data, creditCard);
        CreditCard updatedCard = creditCardRepository.save(creditCard);

        return cardMapper.toResponseDTO(updatedCard);
    }

    public void delete(UUID cardId) {
        CreditCard creditCard = cardSelector.getCreditCardById(cardId);
        creditCardRepository.delete(creditCard);
    }

    public void restore(UUID cardId) {
        CreditCard creditCard = cardSelector.getCreditCardByIdIncludingDeleted(cardId);

        creditCardValidatorService.validateRestoration(creditCard);

        creditCard.setDeleted(false);
        creditCardRepository.save(creditCard);
    }
}
