package com.emmanuelfinance.transaction.transaction.kafka;

import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionCreatedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionDeletedAndRestoreEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionUpdatedEvent;
import com.emmanuelfinance.shared.kafka.KafkaFailureHandler;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionProducer implements KafkaFailureHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_CREATED_TOPIC = "transaction-created-topic";
    private static final String INVOICE_PAYMENT_CREATED_TOPIC = "invoice-payment-created-topic";
    private static final String TRANSACTION_UPDATED_TOPIC = "transaction-updated-topic";
    private static final String TRANSACTION_DELETED_TOPIC = "transaction-deleted-topic";
    private static final String TRANSACTION_RESTORE_TOPIC = "transaction-restore-topic";
    private static final String INVOICE_PAYMENT_FAILED_TOPIC = "invoice-payment-failed-topic";
    private static final long KAFKA_TIMEOUT_SECONDS = 5;

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        log.info("Publicando evento de criacao de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_CREATED_TOPIC, event.transactionId().toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }

    public void publishInvoicePaymentCreated(TransactionCreatedEvent event) {
        log.info("Publicando evento de criacao de transacao (pagamento de fatura) no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(INVOICE_PAYMENT_CREATED_TOPIC, event.transactionId().toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }

    public void publishTransactionUpdated(TransactionUpdatedEvent event) {
        log.info("Publicando evento de atualizacao de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_UPDATED_TOPIC, event.transactionId().toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }

    public void publishTransactionDeleted(TransactionDeletedAndRestoreEvent event) {
        log.info("Publicando evento de exclusão de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_DELETED_TOPIC, event.transactionId().toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }

    public void publishTransactionRestore(TransactionDeletedAndRestoreEvent event) {
        log.info("Publicando evento de recuperação de transacao no Kafka ID: {}", event.transactionId());
        kafkaTemplate.send(TRANSACTION_RESTORE_TOPIC, event.transactionId().toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }

    public void publishInvoicePaymentFailed(InvoicePaymentFailedEvent event) {
        log.info("Publicando evento de falha no pagamento da fatura: {}", event.invoiceId());
        kafkaTemplate.send(INVOICE_PAYMENT_FAILED_TOPIC, event.invoiceId().toString(), event)
                .orTimeout(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
    }

    @Override
    public void onFailure(String topic, Object event) {
        if (event instanceof InvoicePaymentDTO payment) {
            publishInvoicePaymentFailed(new InvoicePaymentFailedEvent(
                    payment.paymentId(), payment.creditCardId(), payment.invoiceId(), payment.totalPaid()));
        }
    }
}
