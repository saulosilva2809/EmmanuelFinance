package com.emmanuelfinance.creditcard.outbox;

import com.emmanuelfinance.creditcard.creditcard.CreditCard;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OutboxEventFactory outboxEventFactory;

    public void invoicePayment(InvoicePaymentDTO invoicePaymentDTO) {
        register(outboxEventFactory.createdInvoicePayment(invoicePaymentDTO));
    }

    // create, update, restore and update limit
    public void creditCardChanged(CreditCard creditCard) {
        register(outboxEventFactory.creditCardSnapshot(creditCard, false));
    }

    // delete
    public void creditCardDeleted(CreditCard creditCard) {
        register(outboxEventFactory.creditCardSnapshot(creditCard, true));
    }

    private void register(OutboxEvent... outboxEvents) {
        outboxEventRepository.saveAll(List.of(outboxEvents));

        for (OutboxEvent outboxEvent : outboxEvents) {
            log.info("Outbox: {} registrado para o id {} (aguardando o commit)",
                    outboxEvent.getType(), outboxEvent.getAggregateId());
        }

        applicationEventPublisher.publishEvent(new OutboxEventSavedDTO());
    }
}
