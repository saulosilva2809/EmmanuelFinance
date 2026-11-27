package com.emmanuelfinance.shared.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedEventService {

    private final JdbcTemplate jdbcTemplate;

    // devolve falso quando o aviso já foi tratado. o registro entra na mesma transação da mudança, então um erro
    // desfaz os dois, e a chave primária barra duas entregas ao mesmo tempo
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean register(UUID eventId) {
        // aviso antigo, publicado antes de existir o id
        if (eventId == null) {
            return true;
        }

        Integer found = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_event WHERE event_id = ?", Integer.class, eventId);
        if (found != null && found > 0) {
            return false;
        }

        jdbcTemplate.update("INSERT INTO processed_event (event_id, processed_at) VALUES (?, ?)",
                eventId, Timestamp.valueOf(LocalDateTime.now()));
        return true;
    }
}
