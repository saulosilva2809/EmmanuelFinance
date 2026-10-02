package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.exceptions.CreditCardDomainException;
import com.emmanuelfinance.creditcard.exceptions.CreditCardErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardBalanceService {

    private void validateLimit(BigDecimal newLimit, BigDecimal newAvailableLimit) {
        if (newLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new CreditCardDomainException(CreditCardErrorCode.THE_CARD_LIMIT_CANNOT_BE_NEGATIVE);
        }

        if (newAvailableLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new CreditCardDomainException(CreditCardErrorCode.THE_AVAILABLE_LIMIT_CANT_BE_NEGATIVE);
        }
    }

    @Transactional
    public void updateAvailableLimit(CreditCard creditCard, UpdateCreditCardDTO data) {
        BigDecimal oldLimit = creditCard.getCreditLimit();
        BigDecimal newLimit = data.creditLimit();

        BigDecimal difference = newLimit.subtract(oldLimit);
        BigDecimal newAvailableLimit = creditCard.getAvailableLimit().add(difference);

        validateLimit(newLimit, newAvailableLimit);

        creditCard.setAvailableLimit(newAvailableLimit);
        creditCard.setCreditLimit(newLimit);
    }
}