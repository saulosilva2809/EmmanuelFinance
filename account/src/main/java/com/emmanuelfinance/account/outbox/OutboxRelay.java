package com.emmanuelfinance.account.outbox;

import com.emmanuelfinance.account.outbox.dto.AccountOwnerDTO;
import com.emmanuelfinance.account.outbox.dto.OutboxEventSavedDTO;
import com.emmanuelfinance.account.query.AccountDocument;
import com.emmanuelfinance.account.query.AccountProjector;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final String ACCOUNT_EVENTS_TOPIC = "account-events";
    private static final long SAFETY_SWEEP_SECONDS = 30;
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, AccountEventDTO> kafkaTemplate;
    private final AccountProjector accountProjector;
    private final AccountCache accountCache;
    private final ObjectMapper objectMapper;

    // uma única thread: os envios acontecem um de cada vez e na ordem em que foram registrados
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

    // só depois do commit, antes disso as linhas ainda não existem para as outras conexões
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOutboxEventSaved(OutboxEventSavedDTO event) {
        executor.execute(this::drain);
    }

    // processa tudo o que está pendente, em lotes, até esvaziar ou até uma linha falhar
    void drain() {
        try {
            List<OutboxEvent> pending = outboxEventRepository.findTop100ByOrderByCreatedAtAsc();

            if (pending.isEmpty()) {
                log.debug("Outbox: nada pendente");
            }

            while (!pending.isEmpty()) {
                log.info("Outbox: {} pendência(s) para processar", pending.size());

                for (OutboxEvent outboxEvent : pending) {
                    try {
                        dispatch(outboxEvent);
                    } catch (Exception e) {
                        // a linha que falhou continua no Outbox e será tentada na próxima rodada
                        log.error("Outbox: falha ao entregar {} da conta {}. A linha continua pendente e as seguintes aguardam (nova tentativa em até {}s)",
                                outboxEvent.getType(), outboxEvent.getAggregateId(), SAFETY_SWEEP_SECONDS, e);
                        return;
                    }

                    outboxEventRepository.delete(outboxEvent);
                    log.info("Outbox: {} da conta {} entregue e removido do Outbox",
                            outboxEvent.getType(), outboxEvent.getAggregateId());
                }
                pending = outboxEventRepository.findTop100ByOrderByCreatedAtAsc();
            }
        } catch (Exception e) {
            log.error("Outbox: falha ao ler ou apagar no banco, as pendências serão tentadas de novo", e);
        }
    }

    private void dispatch(OutboxEvent outboxEvent) throws JsonProcessingException {
        switch (outboxEvent.getType()) {
            case ACCOUNT_EVENT -> {
                AccountEventDTO payload = objectMapper.treeToValue(outboxEvent.getPayload(), AccountEventDTO.class);
                // espera a confirmação do Kafka: se falhar, lança exceção e a linha continua pendente
                kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, outboxEvent.getAggregateId(), payload)
                        .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .join();
                log.info("Outbox: aviso {} da conta {} confirmado pelo Kafka (tópico {})",
                        payload.status(), outboxEvent.getAggregateId(), ACCOUNT_EVENTS_TOPIC);
            }
            case ACCOUNT_CHANGED -> {
                AccountDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), AccountDocument.class);
                accountProjector.project(snapshot);
            }
            case ACCOUNT_OWNER_CACHED -> {
                AccountOwnerDTO owner = objectMapper.treeToValue(outboxEvent.getPayload(), AccountOwnerDTO.class);
                accountCache.saveAccountOwner(owner.accountId(), owner.userId());
                log.info("Outbox: dono da conta {} gravado no cache (Redis)", owner.accountId());
            }
        }
    }
}
