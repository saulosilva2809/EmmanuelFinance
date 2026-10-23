package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.Account;
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

    // criação de conta
    public void accountCreated(Account account) {
        register(
                // dispara um evento de conta criada
                outboxEventFactory.accountCreatedEvent(account),
                // dispara um evento de "foto" para o mongo
                outboxEventFactory.accountSnapshot(account, false),
                // dispara um evento para salvar em cache a conta e o userId (account:account_uuid=userId)
                outboxEventFactory.accountOwner(account)
        );
    }

    public void accountUpdated(Account account) {
        register(outboxEventFactory.accountSnapshot(account, false));
    }

    public void accountDeleted(Account account) {
        register(
                // evento de conta deletada
                outboxEventFactory.accountDeletedEvent(account),
                outboxEventFactory.accountSnapshot(account, true)
        );
    }

    public void accountRestored(Account account) {
        register(
                // evento de conta restaurada
                outboxEventFactory.accountRestoredEvent(account),
                outboxEventFactory.accountSnapshot(account, false)
        );
    }

    public void accountBalanceChanged(Account account) {
        // evento para quando o valor da conta for alterado
        register(outboxEventFactory.accountSnapshot(account, false));
    }

    private void register(OutboxEvent... outboxEvents) {
        // salva os eventos no Outbox
        outboxEventRepository.saveAll(List.of(outboxEvents));

        for (OutboxEvent outboxEvent : outboxEvents) {
            log.info("Outbox: {} registrado para a conta {} (aguardando o commit)",
                    outboxEvent.getType(), outboxEvent.getAggregateId());
        }

        // acorda o relay uma vez por operação, mas só depois do commit (ele escuta com AFTER_COMMIT)
        applicationEventPublisher.publishEvent(new OutboxEventSavedDTO());
    }
}
