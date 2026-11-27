package com.emmanuelfinance.shared.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedEventServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ProcessedEventService processedEventService;

    private final UUID eventId = UUID.randomUUID();

    private void stubFound(int count) {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(eventId))).thenReturn(count);
    }

    @Test
    void shouldRegisterANewEvent() {
        stubFound(0);

        assertTrue(processedEventService.register(eventId));

        verify(jdbcTemplate).update(startsWith("INSERT INTO processed_event"), eq(eventId), any());
    }

    @Test
    void shouldRefuseARepeatedEvent() {
        stubFound(1);

        assertFalse(processedEventService.register(eventId));

        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }

    @Test
    void shouldAcceptAnOldMessageWithoutId() {
        assertTrue(processedEventService.register(null));

        verifyNoInteractions(jdbcTemplate);
    }
}
