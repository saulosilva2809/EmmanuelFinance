package com.emmanuelfinance.transaction.transfer.kafka;

import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferFailedEvent;
import com.emmanuelfinance.transaction.transfer.services.TransferFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferListener {

    private final TransferFailureService transferFailureService;

    // a conta avisa por aqui a transferência que não conseguiu aplicar
    @KafkaListener(topics = "transfer-failed-topic", groupId = "transaction-service-group")
    public void handleTransferFailed(TransferFailedEvent event) {
        log.info("Falha na operação {} da transferência {}", event.operation(), event.transferId());

        transferFailureService.compensate(event);
    }
}
