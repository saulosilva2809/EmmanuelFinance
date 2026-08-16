package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CreditCardTestDataBuilder {

    public static CreateCreditCardDTO createCardDTO() {
        return new CreateCreditCardDTO(
                UUID.randomUUID(),
                "Cartão de Crédito Nubank",
                BanksEnum.NUBANK,
                new BigDecimal(10000),
                17,
                24
        );
    }

    public static CreditCard createEntity(CreateCreditCardDTO inputDto) {
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
        return creditCard;
    }

    public static ResponseCreditCardDTO responseCategoryDTO(CreditCard creditCard, AccountSummaryDTO accountSummary) {
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

    public static AccountSummaryDTO accountSummaryDTO(UUID accountId) {
        return new AccountSummaryDTO(
                accountId,
                "Conta Corrente",
                false
        );
    }
}