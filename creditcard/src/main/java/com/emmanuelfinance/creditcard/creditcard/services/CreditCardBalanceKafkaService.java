package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceService;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCardBalanceKafkaService {

    private final CreditCardSelector creditCardSelector;
    private final InvoiceService invoiceService;
    private final CreditCardRepository creditCardRepository;

    @Transactional
    public void processTransactionCreate(TransactionCreatedEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().subtract(event.amount()));

        creditCardRepository.saveAndFlush(creditCard);
        invoiceService.findOrCreate(event);
    }

    @Transactional
    public void processTransactionDeletion(TransactionDeletedAndRestoreEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().add(event.amount()));

        log.info("NOVO LIMITE DISPONÍVEL: {}", creditCard.getAvailableLimit());
        creditCardRepository.saveAndFlush(creditCard);
        invoiceService.delete(event);
    }

    @Transactional
    public void processTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().subtract(event.amount()));

        creditCardRepository.saveAndFlush(creditCard);
        invoiceService.restore(event);
    }
}