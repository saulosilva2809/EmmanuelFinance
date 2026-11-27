package com.emmanuelfinance.shared.kafka;

import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaErrorHandlingConfigTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final KafkaFailureHandler failureHandler = mock(KafkaFailureHandler.class);
    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    private final TransactionCreatedEvent event = new TransactionCreatedEvent(
            UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), BigDecimal.TEN, 1,
            TypeTransactionEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now(), UUID.randomUUID()
    );

    @AfterEach
    void closeContext() {
        context.close();
    }

    // sobe o bean do jeito que o spring sobe, com ou sem alguém no serviço que trate a falha
    @SuppressWarnings("unchecked")
    private CommonErrorHandler errorHandler(boolean withHandler) {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        context.registerBean(KafkaTemplate.class, () -> kafkaTemplate);
        if (withHandler) {
            context.registerBean(KafkaFailureHandler.class, () -> failureHandler);
        }
        context.register(KafkaErrorHandlingConfig.class);
        context.refresh();

        return context.getBean(CommonErrorHandler.class);
    }

    private boolean handle(CommonErrorHandler handler, Exception error, Object value) {
        ConsumerRecord<String, Object> record = new ConsumerRecord<>("transaction-created-topic", 0, 0L, null, value);

        return handler.handleOne(error, record, mock(Consumer.class), mock(MessageListenerContainer.class));
    }

    @SuppressWarnings("unchecked")
    private String deadLetterTopic() {
        ArgumentCaptor<ProducerRecord<Object, Object>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(sent.capture());
        return sent.getValue().topic();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSendADomainErrorToDeadLetterAndThenMarkTheTransactionAsFailedWithoutRetrying() {
        boolean recovered = handle(errorHandler(true), new TransactionNotFound(), event);

        assertTrue(recovered);
        assertEquals("transaction-created-topic.application.DLT", deadLetterTopic());
        InOrder order = inOrder(kafkaTemplate, failureHandler);
        order.verify(kafkaTemplate).send(any(ProducerRecord.class));
        order.verify(failureHandler).onFailure("transaction-created-topic", event);
    }

    @Test
    void shouldRetryAnUnexpectedErrorBeforeGivingUp() {
        boolean recovered = handle(errorHandler(true), new IllegalStateException("banco fora do ar"), event);

        assertFalse(recovered);
        verifyNoInteractions(failureHandler);
    }

    @Test
    void shouldNotReportAMessageThatCouldNotBeRead() {
        handle(errorHandler(true), new TransactionNotFound(), null);

        verifyNoInteractions(failureHandler);
    }

    @Test
    void shouldSendToDeadLetterInAServiceWithoutAFailureHandler() {
        boolean recovered = handle(errorHandler(false), new TransactionNotFound(), event);

        assertTrue(recovered);
        assertEquals("transaction-created-topic.application.DLT", deadLetterTopic());
    }
}
