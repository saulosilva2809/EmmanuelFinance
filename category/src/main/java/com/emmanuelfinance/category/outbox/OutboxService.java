package com.emmanuelfinance.category.outbox;

import com.emmanuelfinance.category.Category;
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

    public void categoryChanged(Category category) {
        register(outboxEventFactory.categorySnapshot(category, false));
    }

    public void categoryDeleted(Category category) {
        register(outboxEventFactory.categorySnapshot(category, true));
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
