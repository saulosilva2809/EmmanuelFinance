package com.emmanuelfinance.transaction.transaction.outbox;

import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import com.emmanuelfinance.transaction.transaction.Transaction;
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

    public void transactionChanged(Transaction transaction) {
        register(outboxEventFactory.transactionSnapshot(transaction, false));
    }

    public void transactionDeleted(Transaction transaction) {
        register(outboxEventFactory.transactionSnapshot(transaction, true));
    }

    public void transactionCreatedEvent(TransactionCreatedEvent event) {
        register(outboxEventFactory.transactionCreated(event));
    }

    public void invoicePaymentCreatedEvent(TransactionCreatedEvent event) {
        register(outboxEventFactory.invoicePaymentCreated(event));
    }

    public void transactionUpdatedEvent(TransactionUpdatedEvent event) {
        register(outboxEventFactory.transactionUpdated(event));
    }

    public void transactionDeletedEvent(TransactionDeletedAndRestoreEvent event) {
        register(outboxEventFactory.transactionDeleted(event));
    }

    public void transactionRestoredEvent(TransactionDeletedAndRestoreEvent event) {
        register(outboxEventFactory.transactionRestored(event));
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
