package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.*;
import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.CreditCardFiltersDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.exceptions.CheckCardAndAccountBankError;
import com.emmanuelfinance.creditcard.exceptions.RestoreCreditCardError;
import com.emmanuelfinance.shared.annotation.WithDeletedFilter;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
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
    private final CreditCardSelector cardSelector;

    private ResponseCreditCardDTO cardAsDTO(CreditCard data) {
        AccountSummaryDTO account = accountClientCacheService
                .getAccountSummaryById(data.getAccountId());

        return new ResponseCreditCardDTO(
                data.getId(),
                account,
                data.getName(),
                data.getBank(),
                data.getCreditLimit(),
                data.getClosingDay(),
                data.getDueDay(),
                data.getCreatedAt(),
                data.getUpdatedAt(),
                data.isDeleted()
        );
    }

    private void checkCardAndAccountBank(BanksEnum cardBank, UUID accountId) {
        AccountSummaryInternalDTO account = accountClientCacheService.getInternalAccountById(accountId);

        if (!cardBank.equals(account.bank())) {
            throw new CheckCardAndAccountBankError();
        }
    }

    public ResponseCreditCardDTO create(CreateCreditCardDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        accountOwnershipValidator.validate(data.accountId());
        checkCardAndAccountBank(data.bank(), data.accountId());

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
        CreditCard creditCard = cardSelector.getCreditCardById(cardId);
        return cardAsDTO(creditCard);
    }

    @WithDeletedFilter()
    public PageResponseDTO<ResponseCreditCardDTO> list(CreditCardFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<CreditCard> specification = CreditCardSpecification.withFilter(filters, userId, false);
        Page<CreditCard> page = creditCardRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseCreditCardDTO> dtoPage = page.map(this::cardAsDTO);

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

        Page<ResponseCreditCardDTO> dtoPage = page.map(this::cardAsDTO);

        return PageResponseDTO.from(dtoPage);
    }

    public ResponseCreditCardDTO update(UUID cardId, UpdateCreditCardDTO data) {
        CreditCard creditCard = cardSelector.getCreditCardById(cardId);

        if (data.accountId() != null) {
            accountOwnershipValidator.validate(data.accountId());
        }

        cardMapper.updateCreditCardFromDTO(data, creditCard);
        CreditCard updatedCard = creditCardRepository.save(creditCard);

        return cardAsDTO(updatedCard);
    }

    public void delete(UUID cardId) {
        CreditCard creditCard = cardSelector.getCreditCardById(cardId);
        creditCardRepository.delete(creditCard);
    }

    public void restore(UUID cardId) {
        CreditCard creditCard = cardSelector.getCreditCardByIdIncludingDeleted(cardId);

        if (!creditCard.isDeleted()) {
            throw new RestoreCreditCardError();
        }

        creditCard.setDeleted(false);
        creditCardRepository.save(creditCard);
    }
}
