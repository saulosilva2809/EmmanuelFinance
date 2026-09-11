package com.emmanuelfinance.creditcard.services;

import com.emmanuelfinance.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceService;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
public class CreditCardBalanceKafkaService {

    private final CreditCardSelector creditCardSelector;
    private final InvoiceService invoiceService;

    @Transactional
    public void processTransactionCreate(TransactionCreatedEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().subtract(event.amount()));

        invoiceService.findOrCreate(event);
    }

    @Transactional
    public void processTransactionDeletion(TransactionDeletedAndRestoreEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().add(event.amount()));

        invoiceService.delete(event);
    }

    @Transactional
    public void processTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().add(event.amount()));

        invoiceService.restore(event);
    }
}