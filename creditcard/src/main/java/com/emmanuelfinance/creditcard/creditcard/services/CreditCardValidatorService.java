package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.exceptions.*;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditCardValidatorService {

    private final AccountClientCacheService accountClientCacheService;
    private final AccountOwnershipValidator accountOwnershipValidator;

    // valida se o usuário que fez a request é o dono da conta informada
    public void validateAccountOwnership(UUID accountId) {
        accountOwnershipValidator.validate(accountId);
    }

    // valida se o banco do cartão é o mesmo banco da conta
    public void validateCardAndAccountBank(BanksEnum cardBank, UUID accountId) {
        AccountSummaryInternalDTO account = accountClientCacheService.getInternalAccountById(accountId);

        if (!cardBank.equals(account.bank())) {
            throw new CreditCardDomainException(CreditCardErrorCode.BANK_OF_CARD_AND_ACCOUNT_DIFFERENT);
        }
    }

    // verifica se o cartão está deletado
    public void validateIsCardDeleted(CreditCard creditCard) {
        if (!creditCard.isDeleted()) {
            throw new CreditCardDomainException(CreditCardErrorCode.RESTORE_CARD_NOT_DELETED);
        }
    }

    // verifica se a conta está deletada
    public void validateAccountNotDeleted(UUID accountId) {
        AccountSummaryInternalDTO account = accountClientCacheService.getInternalAccountById(accountId);

        if (Boolean.TRUE.equals(account.deleted())) {
            throw new CreditCardDomainException(CreditCardErrorCode.RESTORE_CARD_WITH_DELETED_ACCOUNT);
        }
    }

    // validações de criação
    public void validateCreation(CreateCreditCardDTO data) {
        validateAccountOwnership(data.accountId());
        validateCardAndAccountBank(data.bank(), data.accountId());
    }

    // validações de alteração: se mudar a conta ou o banco, os dois precisam continuar combinando
    public void validateUpdate(CreditCard creditCard, UpdateCreditCardDTO data) {
        if (data.accountId() == null && data.bank() == null) {
            return;
        }

        UUID accountId = data.accountId() != null ? data.accountId() : creditCard.getAccountId();
        BanksEnum bank = data.bank() != null ? data.bank() : creditCard.getBank();

        validateAccountOwnership(accountId);
        validateCardAndAccountBank(bank, accountId);
    }

    // validações de restauração
    public void validateRestoration(CreditCard creditCard) {
        validateIsCardDeleted(creditCard);
        validateAccountNotDeleted(creditCard.getAccountId());
    }
}
