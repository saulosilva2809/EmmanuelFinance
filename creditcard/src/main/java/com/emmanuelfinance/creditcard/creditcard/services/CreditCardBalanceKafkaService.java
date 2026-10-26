package com.emmanuelfinance.creditcard.creditcard.services;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.creditcard.creditcard.CreditCardRepository;
import com.emmanuelfinance.creditcard.creditcard.CreditCardSelector;
import com.emmanuelfinance.creditcard.invoice.services.InvoiceService;
import com.emmanuelfinance.creditcard.outbox.OutboxService;
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
    private final OutboxService outboxService;


    // altera o limite disponível no cartão após uma transação ser criada
    @Transactional
    public void processTransactionCreate(TransactionCreatedEvent event) {
        // pega o cartão
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        // subtrai no limite
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().subtract(event.amount()));

        // salva no postgres
        creditCardRepository.saveAndFlush(creditCard);
        // salva no outbox para executar ações posteriores e salvar no mongo
        outboxService.creditCardChanged(creditCard);
        // cria ou procura uma fatura referente ao mes da transação criada
        invoiceService.findOrCreate(event);
    }

    // altera o limite disponível no cartão após deletar uma transação
    @Transactional
    public void processTransactionDeletion(TransactionDeletedAndRestoreEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        // restaura o limite
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().add(event.amount()));

        log.info("NOVO LIMITE DISPONÍVEL: {}", creditCard.getAvailableLimit());

        creditCardRepository.saveAndFlush(creditCard);
        outboxService.creditCardChanged(creditCard);

        // deleta invoices
        invoiceService.delete(event);
    }

    // altera o limite disponível no cartão após restaurar uma transação
    @Transactional
    public void processTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        CreditCard creditCard = creditCardSelector.getCreditCardByIdInternal(event.creditCardId());
        // subtrai no limite
        creditCard.setAvailableLimit(creditCard.getAvailableLimit().subtract(event.amount()));

        creditCardRepository.saveAndFlush(creditCard);
        outboxService.creditCardChanged(creditCard);
        // restaura invoices
        invoiceService.restore(event);
    }
}
