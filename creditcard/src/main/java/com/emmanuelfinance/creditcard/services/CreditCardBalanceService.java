package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceService;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardBalanceService {

    @Transactional
    public void updateAvailableLimit(CreditCard creditCard, UpdateCreditCardDTO data) {
        BigDecimal oldLimit = creditCard.getCreditLimit();
        BigDecimal newLimit = data.creditLimit();
        BigDecimal difference = newLimit.subtract(oldLimit);

        // TODO: garantir que AvailableLimit não seja menor que 0

        creditCard.setAvailableLimit(creditCard.getAvailableLimit().add(difference));
    }
}