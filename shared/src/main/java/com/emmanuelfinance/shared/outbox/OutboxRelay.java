package com.emmanuelfinance.shared.outbox;

import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Entrega o que o Outbox do serviço registrou. Não é @Component de propósito: cada serviço que tem um Outbox
// o importa com @Import(OutboxRelay.class). Um @ConditionalOnBean aqui seria avaliado antes dos repositórios
// existirem e o relay nunca seria criado.
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
            List<? extends BaseOutboxEvent> pending = outboxEventRepository.findTop100ByOrderByCreatedAtAsc();

            if (pending.isEmpty()) {
                log.debug("Outbox: nada pendente");
                return;
            }

            while (!pending.isEmpty()) {
                log.info("Outbox: {} pendência(s) para processar", pending.size());

                for (BaseOutboxEvent outboxEvent : pending) {
                    try {
                        processEvent(outboxEvent);
                    } catch (Exception e) {
                        log.error("Outbox: falha ao entregar {} do id {}. A linha continua pendente e as seguintes aguardam (nova tentativa em até {}s)",
                                outboxEvent.getType(), outboxEvent.getAggregateId(), SAFETY_SWEEP_SECONDS, e);
                        return;
                    }

                    deleteEvent(outboxEvent);
                    log.info("Outbox: evento {} (ID: {}) entregue e removido do Outbox",
                            outboxEvent.getType(), outboxEvent.getAggregateId());
                }
                pending = outboxEventRepository.findTop100ByOrderByCreatedAtAsc();
            }
        } catch (Exception e) {
            log.error("Outbox: falha ao ler ou apagar no banco, as pendências serão tentadas de novo", e);
        }
    }

    private void processEvent(BaseOutboxEvent event) throws Exception {
        OutboxDispatcher dispatcher = dispatcherList.stream()
                .filter(d -> d.supports(event.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhum OutboxDispatcher encontrado para o tipo: " + event.getType()));

        dispatcher.dispatch(event);
    }

    @SuppressWarnings("unchecked")
    private void deleteEvent(BaseOutboxEvent event) {
        ((BaseOutboxEventRepository<BaseOutboxEvent>) outboxEventRepository).delete(event);
    }
}
