package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.exceptions.CreditCardDomainException;
import com.emmanuelfinance.creditcard.exceptions.CreditCardErrorCode;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceService;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CreditCardBalanceService {

    private final CreditCardSelector creditCardSelector;
    private final CreditCardRepository creditCardRepository;
    private final InvoiceService invoiceService;

    @Transactional
    public void processTransactionCreate(TransactionCreatedEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());

        if (creditCard.getAvailableLimit().compareTo(event.amount()) < 0) {
            throw new CreditCardDomainException(CreditCardErrorCode.INSUFFICIENT_LIMIT);
        }
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().subtract(event.amount()));
        creditCardRepository.save(creditCard);

        invoiceService.findOrCreate(event);
    }

    @Transactional
    public void processTransactionDeletion(TransactionDeletedAndRestoreEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());

        creditCard.setAvailableLimit(creditCard.getAvailableLimit().add(event.amount()));
        creditCardRepository.save(creditCard);
    }

    @Transactional
    public void processTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());

        if (creditCard.getAvailableLimit().compareTo(event.amount()) < 0) {
            throw new CreditCardDomainException(CreditCardErrorCode.INSUFFICIENT_LIMIT);
        }

        creditCard.setAvailableLimit(creditCard.getAvailableLimit().subtract(event.amount()));
        creditCardRepository.save(creditCard);
    }
}