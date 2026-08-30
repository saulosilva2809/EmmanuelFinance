package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.exceptions.CheckCardAndAccountBankError;
import com.emmanuelfinance.creditcard.exceptions.RestoreCreditCardError;
import com.emmanuelfinance.creditcard.exceptions.RestoreItemNotDeletedException;
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

    public void validateAccountOwnership(UUID accountId) {
        accountOwnershipValidator.validate(accountId);
    }

    public void validateCardAndAccountBank(BanksEnum cardBank, UUID accountId) {
        AccountSummaryInternalDTO account = accountClientCacheService.getInternalAccountById(accountId);

        if (!cardBank.equals(account.bank())) {
            throw new CheckCardAndAccountBankError();
        }
    }

    public void validateIsCardDeleted(CreditCard creditCard) {
        if (!creditCard.isDeleted()) {
            throw new RestoreItemNotDeletedException();
        }
    }

    public void validateAccountNotDeleted(UUID accountId) {
        AccountSummaryInternalDTO account = accountClientCacheService.getInternalAccountById(accountId);

        if (Boolean.TRUE.equals(account.deleted())) {
            throw new RestoreCreditCardError();
        }
    }

    public void validateCreation(CreateCreditCardDTO data) {
        validateAccountOwnership(data.accountId());
        validateCardAndAccountBank(data.bank(), data.accountId());
    }

    public void validateRestoration(CreditCard creditCard) {
        validateIsCardDeleted(creditCard);
        validateAccountNotDeleted(creditCard.getAccountId());
    }
}