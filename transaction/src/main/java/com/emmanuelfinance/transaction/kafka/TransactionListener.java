package com.emmanuelfinance.transaction.kafka;

import com.emmanuelfinance.shared.modules.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.Transaction;
import com.emmanuelfinance.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.TransactionSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionListener {

    private final TransactionSelector transactionSelector;
    private final TransactionRepository transactionRepository;

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
}