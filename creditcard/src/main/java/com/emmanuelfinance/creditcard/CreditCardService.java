package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.exceptions.CreditCardNotFound;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
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
    private final AccountClientCacheService accountClientCacheService;
    private final SecurityUtils securityUtils;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardMapper cardMapper;

    private ResponseCreditCardDTO cardAsDTO(CreditCard data) {
        AccountSummaryDTO account = accountClientCacheService
                .getInternalAccountById(data.getAccountId());

        return new ResponseCreditCardDTO(
                data.getId(),
                account,
                data.getName(),
                data.getCreditLimit(),
                data.getClosingDay(),
                data.getDueDay(),
                data.getCreatedAt(),
                data.getUpdatedAt()
        );
    }

    private CreditCard getCreditCardById(UUID cardId) {
        UUID userId = securityUtils.getCurrentUserId();
        CreditCard creditCard = creditCardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CreditCardNotFound());

        return creditCard;
    }

    public ResponseCreditCardDTO create(CreateCreditCardDTO data) {
        UUID userId = securityUtils.getCurrentUserId();
        accountOwnershipValidator.validate(data.accountId());

        CreditCard creditCard = new CreditCard();

        creditCard.setUserId(userId);
        creditCard.setAccountId(data.accountId());
        creditCard.setName(data.name());
        creditCard.setBank(data.bank());
        Optional.ofNullable(data.creditLimit())
                .ifPresent(creditLimit -> creditCard.setCreditLimit(creditLimit));
        creditCard.setClosingDay(data.closingDay());
        creditCard.setDueDay(data.dueDay());

        CreditCard savedCreditCard = creditCardRepository.save(creditCard);

        return cardAsDTO(savedCreditCard);
    }

    public ResponseCreditCardDTO view(UUID cardId) {
        CreditCard creditCard = getCreditCardById(cardId);
        return cardAsDTO(creditCard);
    }

    public PageResponseDTO<ResponseCreditCardDTO> list(CreditCardFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<CreditCard> specification = CreditCardSpecification.withFilter(filters, userId);
        Page<CreditCard> page = creditCardRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseCreditCardDTO> dtoPage = page.map(this::cardAsDTO);

        return PageResponseDTO.from(dtoPage);
    }

    public ResponseCreditCardDTO update(UUID cardId, UpdateCreditCardDTO data) {
        CreditCard creditCard = getCreditCardById(cardId);

        if (data.accountId() != null) {
            accountOwnershipValidator.validate(data.accountId());
        }

        cardMapper.updateCreditCardFromDTO(data, creditCard);
        CreditCard updatedCard = creditCardRepository.save(creditCard);

        return cardAsDTO(updatedCard);
    }

    public void delete(UUID cardId) {
        CreditCard creditCard = getCreditCardById(cardId);
        creditCardRepository.delete(creditCard);
    }
}
