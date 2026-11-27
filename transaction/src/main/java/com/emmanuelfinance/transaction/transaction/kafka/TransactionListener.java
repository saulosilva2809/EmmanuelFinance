package com.emmanuelfinance.transaction.transaction.kafka;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionFailedEvent;
import com.emmanuelfinance.transaction.transaction.services.TransactionFailureService;
import com.emmanuelfinance.transaction.transaction.services.TransactionServiceInternal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionListener {

    private final TransactionFailureService transactionFailureService;
    private final TransactionServiceInternal transactionServiceInternal;

    // a conta ou o cartão avisa por aqui o que não conseguiu aplicar
    @KafkaListener(topics = "transaction-failed-topic", groupId = "transaction-service-group")
    public void handleTransactionFailed(TransactionFailedEvent event) {
        log.info("Falha na operação {} da transação {}", event.operation(), event.transactionId());

        transactionFailureService.compensate(event);
    }

    @KafkaListener(
            topics = "created-invoice-payment-topic",
            groupId = "transaction-service-group"
    )
    public void handleCreditCardInvoicePayment(InvoicePaymentDTO data) {
        log.info("Pagamento de fatura do cartão de crédito. Credit Card ID: {}", data.creditCardId());

        transactionServiceInternal.createTransactionRelatedToInvoicePayment(data);
    }
}
