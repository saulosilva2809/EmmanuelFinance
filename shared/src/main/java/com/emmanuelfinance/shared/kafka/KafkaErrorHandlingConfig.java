package com.emmanuelfinance.shared.kafka;

import com.emmanuelfinance.config.exceptions.APIException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

    private static final long RETRY_INTERVAL_MILLIS = 2000L;
    private static final long MAX_RETRIES = 3L;

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<?, ?> kafkaTemplate,
            ObjectProvider<KafkaFailureHandler> failureHandler,
            @Value("${spring.application.name:application}") String applicationName
    ) {
        DeadLetterPublishingRecoverer deadLetter = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + "." + applicationName + ".DLT", -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer(deadLetter, failureHandler),
                new FixedBackOff(RETRY_INTERVAL_MILLIS, MAX_RETRIES));
        handler.addNotRetryableExceptions(APIException.class);
        return handler;
    }

    private ConsumerRecordRecoverer recoverer(
            DeadLetterPublishingRecoverer deadLetter,
            ObjectProvider<KafkaFailureHandler> failureHandler
    ) {
        return (record, exception) -> {
            log.error("Mensagem do tópico {} (offset {}) não foi processada e vai para a DLQ",
                    record.topic(), record.offset(), exception);
            deadLetter.accept(record, exception);

            if (record.value() != null) {
                failureHandler.ifAvailable(handler -> handler.onFailure(record.topic(), record.value()));
            }
        };
    }
}
