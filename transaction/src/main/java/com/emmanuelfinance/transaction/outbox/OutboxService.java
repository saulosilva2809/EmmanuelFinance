package com.emmanuelfinance.transaction.outbox;

import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    // grava a linha na transação de quem chamou e acorda o relay, que só entrega depois do commit
    public void register(OutboxEvent outboxEvent) {
        outboxEventRepository.save(outboxEvent);
        log.debug("Outbox: {} registrado para o id {} (aguardando o commit)",
                outboxEvent.getType(), outboxEvent.getAggregateId());

        applicationEventPublisher.publishEvent(new OutboxEventSavedDTO());
    }
}
