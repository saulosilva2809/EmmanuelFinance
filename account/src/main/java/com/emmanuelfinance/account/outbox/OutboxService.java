package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.outbox.dto.OutboxEventSavedDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
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

    public void accountCreated(Account account) {
        register(
                outboxEventFactory.accountEvent(account, StatusEventEnum.CREATED),
                outboxEventFactory.accountSnapshot(account, false),
                outboxEventFactory.accountOwner(account)
        );
    }

    public void accountUpdated(Account account) {
        register(outboxEventFactory.accountSnapshot(account, false));
    }

    public void accountDeleted(Account account) {
        register(
                outboxEventFactory.accountEvent(account, StatusEventEnum.DELETED),
                outboxEventFactory.accountSnapshot(account, true)
        );
    }

    public void accountRestored(Account account) {
        register(
                outboxEventFactory.accountEvent(account, StatusEventEnum.RESTORE),
                outboxEventFactory.accountSnapshot(account, false)
        );
    }

    public void accountBalanceChanged(Account account) {
        register(outboxEventFactory.accountSnapshot(account, false));
    }

    private void register(OutboxEvent... outboxEvents) {
        outboxEventRepository.saveAll(List.of(outboxEvents));

        for (OutboxEvent outboxEvent : outboxEvents) {
            log.info("Outbox: {} registrado para a conta {} (aguardando o commit)",
                    outboxEvent.getType(), outboxEvent.getAggregateId());
        }

        // acorda o relay uma vez por operação, mas só depois do commit (ele escuta com AFTER_COMMIT)
        applicationEventPublisher.publishEvent(new OutboxEventSavedDTO());
    }
}
