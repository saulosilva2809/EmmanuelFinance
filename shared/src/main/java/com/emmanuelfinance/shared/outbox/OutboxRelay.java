package com.emmanuelfinance.shared.outbox;

import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class OutboxRelay {

    private static final long SAFETY_SWEEP_SECONDS = 30;

    private final BaseOutboxEventRepository<? extends BaseOutboxEvent> outboxEventRepository;
    private final List<OutboxDispatcher> dispatcherList;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "outbox-relay");
        thread.setDaemon(true);
        return thread;
    });

    // uma linha que falhou segura só as próximas do mesmo destino e do mesmo registro, para manter a ordem
    private record BlockedKey(OutboxDispatcher dispatcher, String aggregateId) {
    }

    @PostConstruct
    void start() {
        executor.scheduleWithFixedDelay(this::drain, 0, SAFETY_SWEEP_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() {
        executor.shutdown();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOutboxEventSaved(OutboxEventSavedDTO event) {
        executor.execute(this::drain);
    }

    public void drain() {
        try {
            // linhas que falharam ou ficaram esperando nesta varredura: saem das próximas buscas, senão
            // as mesmas 100 linhas presas voltariam sempre e esconderiam as que estão atrás delas
            Set<UUID> skipped = new HashSet<>();
            Set<BlockedKey> blocked = new HashSet<>();

            List<? extends BaseOutboxEvent> pending = nextPending(skipped);

            if (pending.isEmpty()) {
                log.debug("Outbox: nada pendente");
                return;
            }

            while (!pending.isEmpty()) {
                log.info("Outbox: {} pendência(s) para processar", pending.size());

                for (BaseOutboxEvent outboxEvent : pending) {
                    OutboxDispatcher dispatcher = dispatcherOf(outboxEvent);
                    BlockedKey key = new BlockedKey(dispatcher, outboxEvent.getAggregateId());

                    if (blocked.contains(key)) {
                        skipped.add(outboxEvent.getId());
                        continue;
                    }

                    try {
                        processEvent(outboxEvent, dispatcher);
                    } catch (Exception e) {
                        // só este destino fica esperando: um mongo fora do ar não pode travar os avisos do kafka
                        log.error("Outbox: falha ao entregar {} do id {}. A linha continua pendente e as seguintes do mesmo destino e do mesmo id aguardam (nova tentativa em até {}s)",
                                outboxEvent.getType(), outboxEvent.getAggregateId(), SAFETY_SWEEP_SECONDS, e);
                        blocked.add(key);
                        skipped.add(outboxEvent.getId());
                        continue;
                    }

                    deleteEvent(outboxEvent);
                    log.info("Outbox: evento {} (ID: {}) entregue e removido do Outbox",
                            outboxEvent.getType(), outboxEvent.getAggregateId());
                }
                pending = nextPending(skipped);
            }
        } catch (Exception e) {
            log.error("Outbox: falha ao ler ou apagar no banco, as pendências serão tentadas de novo", e);
        }
    }

    private List<? extends BaseOutboxEvent> nextPending(Set<UUID> skipped) {
        if (skipped.isEmpty()) {
            return outboxEventRepository.findTop100ByOrderByCreatedAtAsc();
        }
        return outboxEventRepository.findTop100ByIdNotInOrderByCreatedAtAsc(skipped);
    }

    // devolve null quando nenhum dispatcher atende o tipo; o processEvent trata isso como falha
    private OutboxDispatcher dispatcherOf(BaseOutboxEvent event) {
        return dispatcherList.stream()
                .filter(d -> d.supports(event.getType()))
                .findFirst()
                .orElse(null);
    }

    private void processEvent(BaseOutboxEvent event, OutboxDispatcher dispatcher) throws Exception {
        if (dispatcher == null) {
            throw new IllegalStateException("Nenhum OutboxDispatcher encontrado para o tipo: " + event.getType());
        }

        dispatcher.dispatch(event);
    }

    @SuppressWarnings("unchecked")
    private void deleteEvent(BaseOutboxEvent event) {
        ((BaseOutboxEventRepository<BaseOutboxEvent>) outboxEventRepository).delete(event);
    }
}
