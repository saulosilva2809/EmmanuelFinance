package com.emmanuelfinance.transaction.kafka;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.kafka.dto.TransactionCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TransactionProducer transactionProducer;

    private TransactionCreatedEvent event() {
        return new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), 1, TypeEnum.EXPENSE, StatusTransactionEnum.PAID, LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Deve publicar a transação criada em transaction-created-topic usando o id como chave")
    void shouldPublishTransactionCreated() {
        TransactionCreatedEvent event = event();

        transactionProducer.publishTransactionCreated(event);

        verify(kafkaTemplate, times(1)).send("transaction-created-topic", event.transactionId().toString(), event);
    }

    @Test
    @DisplayName("Deve publicar o pagamento de fatura em invoice-payment-created-topic usando o id como chave")
    void shouldPublishInvoicePaymentCreated() {
        TransactionCreatedEvent event = event();

        transactionProducer.publishInvoicePaymentCreated(event);

        verify(kafkaTemplate, times(1))
                .send("invoice-payment-created-topic", event.transactionId().toString(), event);
    }
}
