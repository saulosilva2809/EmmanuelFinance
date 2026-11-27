package com.emmanuelfinance.transaction.transfer.services;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferCreatedEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferDeletedAndRestoreEventDTO;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferUpdatedEventDTO;
import com.emmanuelfinance.transaction.transfer.outbox.TransferOutbox;
import com.emmanuelfinance.transaction.transfer.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

// monta os avisos para o account. transferência com falha nunca mexeu no saldo, então não avisa nada
@Service
@RequiredArgsConstructor
public class TransferEventsService {

    private final TransferOutbox transferOutbox;

    public void publishCreatedIfPaid(Transfer transfer) {
        if (isPaid(transfer)) {
            transferOutbox.transferCreatedEvent(new TransferCreatedEventDTO(
                    transfer.getId(),
                    transfer.getSourceAccountId(),
                    transfer.getDestinationAccountId(),
                    transfer.getAmount(),
                    UUID.randomUUID()
            ));
        }
    }

    // só avisa quando mudou algo que mexe no saldo
    public void publishUpdatedIfPaid(Transfer transfer, UUID oldSourceId, UUID oldDestinationId, BigDecimal oldAmount) {
        boolean balanceChanged = !oldSourceId.equals(transfer.getSourceAccountId())
                || !oldDestinationId.equals(transfer.getDestinationAccountId())
                || oldAmount.compareTo(transfer.getAmount()) != 0;

        if (isPaid(transfer) && balanceChanged) {
            transferOutbox.transferUpdatedEvent(new TransferUpdatedEventDTO(
                    transfer.getId(),
                    oldSourceId,
                    oldDestinationId,
                    oldAmount,
                    transfer.getSourceAccountId(),
                    transfer.getDestinationAccountId(),
                    transfer.getAmount(),
                    UUID.randomUUID()
            ));
        }
    }

    public void publishDeletedIfPaid(Transfer transfer) {
        if (isPaid(transfer)) {
            transferOutbox.transferDeletedEvent(deletedAndRestoreEvent(transfer));
        }
    }

    public void publishRestoredIfPaid(Transfer transfer) {
        if (isPaid(transfer)) {
            transferOutbox.transferRestoredEvent(deletedAndRestoreEvent(transfer));
        }
    }

    private boolean isPaid(Transfer transfer) {
        return transfer.getStatus() == StatusTransactionEnum.PAID;
    }

    private TransferDeletedAndRestoreEventDTO deletedAndRestoreEvent(Transfer transfer) {
        return new TransferDeletedAndRestoreEventDTO(
                transfer.getId(),
                transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(),
                transfer.getAmount(),
                UUID.randomUUID()
        );
    }
}
