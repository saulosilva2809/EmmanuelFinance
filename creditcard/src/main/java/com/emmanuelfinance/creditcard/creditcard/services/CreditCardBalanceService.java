package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.creditcard.exceptions.CreditCardDomainException;
import com.emmanuelfinance.creditcard.creditcard.exceptions.CreditCardErrorCode;
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
        // verifica se o novo limite é menor que 0
        if (newLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new CreditCardDomainException(CreditCardErrorCode.THE_CARD_LIMIT_CANNOT_BE_NEGATIVE);
        }

        // verifica se o novo limite disponível é menor que 0
        if (newAvailableLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new CreditCardDomainException(CreditCardErrorCode.THE_AVAILABLE_LIMIT_CANT_BE_NEGATIVE);
        }
    }

    // atualiza o limite disponível
    @Transactional
    public void updateAvailableLimit(CreditCard creditCard, UpdateCreditCardDTO data) {
        // limite antigo -> limite atual do cartão
        BigDecimal oldLimit = creditCard.getCreditLimit();
        // novo limite -> limite informado pelo usuário
        BigDecimal newLimit = data.creditLimit();

        // diferença entre os dois limites
        BigDecimal difference = newLimit.subtract(oldLimit);
        // calcula o novo limite disponível (somando com a diferença)
        BigDecimal newAvailableLimit = creditCard.getAvailableLimit().add(difference);

        // validações
        validateLimit(newLimit, newAvailableLimit);

        // define o novo limite disponível
        creditCard.setAvailableLimit(newAvailableLimit);
        // define o novo limite do cartão
        creditCard.setCreditLimit(newLimit);
    }
}
