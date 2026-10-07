package com.emmanuelfinance.transaction.kafka;

import com.emmanuelfinance.shared.enums.TypeEnum;
import com.emmanuelfinance.shared.modules.payment.InvoicePaymentDTO;
import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.TransactionSelector;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.services.TransactionService;
import com.emmanuelfinance.transaction.services.TransactionServiceInternal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionListener {

    private final TransactionSelector transactionSelector;
    private final TransactionRepository transactionRepository;
    private final TransactionServiceInternal transactionServiceInternal;

    @KafkaListener(
            topics = "transaction-failed-topic",
            groupId = "transaction-service-group",
            properties = {
                    "spring.json.value.default.type=java.util.UUID"
            }
    )
    public void handleTransactionFailed(UUID transactionId) {
        log.info("Atualizando status da transação {} para FAILED", transactionId);

        Transaction transaction = transactionSelector.getTransactionByIdInternal(transactionId);
        transaction.setStatus(StatusTransactionEnum.FAILED);
        transactionRepository.save(transaction);
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