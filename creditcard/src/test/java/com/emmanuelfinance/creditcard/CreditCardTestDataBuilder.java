package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CreditCardTestDataBuilder {

    public static CreateCreditCardDTO createCardDTO() {
        return new CreateCreditCardDTO(
                UUID.randomUUID(),
                "Cartão de Crédito C6",
                BanksEnum.C6_BANK,
                new BigDecimal(10000),
                17,
                24
        );
    }

    public static CreditCard createEntity(CreateCreditCardDTO inputDto, boolean deleted) {
        CreditCard creditCard = new CreditCard();
        creditCard.setId(UUID.randomUUID());
        creditCard.setAccountId(inputDto.accountId());
        creditCard.setName(inputDto.name());
        creditCard.setBank(inputDto.bank());
        creditCard.setCreditLimit(inputDto.creditLimit());
        creditCard.setClosingDay(inputDto.closingDay());
        creditCard.setDueDay(inputDto.dueDay());
        creditCard.setCreatedAt(LocalDateTime.now());
        creditCard.setUpdatedAt(null);
        creditCard.setDeleted(deleted);
        return creditCard;
    }

    public static ResponseCreditCardDTO responseCategoryDTO(CreditCard creditCard, AccountSummaryInternalDTO accountSummaryInternal) {
        AccountSummaryDTO accountSummary = accountSummaryDTO(accountSummaryInternal);

        return new ResponseCreditCardDTO(
                creditCard.getId(),
                accountSummary,
                creditCard.getName(),
                creditCard.getBank(),
                creditCard.getCreditLimit(),
                creditCard.getClosingDay(),
                creditCard.getDueDay(),
                creditCard.getCreatedAt(),
                creditCard.getUpdatedAt(),
                creditCard.isDeleted()
        );
    }

    public static AccountSummaryDTO accountSummaryDTO(AccountSummaryInternalDTO internalDTO) {
        return new AccountSummaryDTO(
                internalDTO.id(),
                internalDTO.name(),
                internalDTO.deleted()
        );
    }

    public static AccountSummaryInternalDTO accountSummaryInternalDTO(UUID accountId) {
        return new AccountSummaryInternalDTO(
                accountId,
                "Conta Corrente",
                BanksEnum.C6_BANK,
                false
        );
    }
}