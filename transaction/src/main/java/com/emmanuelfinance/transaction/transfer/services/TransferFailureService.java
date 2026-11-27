package com.emmanuelfinance.transaction.transfer.services;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferFailedEvent;
import com.emmanuelfinance.transaction.transfer.outbox.TransferOutbox;
import com.emmanuelfinance.transaction.transfer.Transfer;
import com.emmanuelfinance.transaction.transfer.TransferRepository;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferDomainException;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferFailureService {

    private final TransferRepository transferRepository;
    private final TransferOutbox transferOutbox;

    // a conta não aplicou a operação, então a transferência volta a mostrar o que de fato aconteceu
    @Transactional
    public void compensate(TransferFailedEvent event) {
        Transfer transfer = transferRepository.findById(event.transferId())
                .orElseThrow(() -> new TransferDomainException(TransferErrorCode.TRANSFER_NOT_FOUND));

        switch (event.operation()) {
            case CREATE -> transfer.setStatus(StatusTransactionEnum.FAILED);
            case UPDATE -> {
                transfer.setSourceAccountId(event.oldSourceAccountId());
                transfer.setDestinationAccountId(event.oldDestinationAccountId());
                transfer.setAmount(event.oldAmount());
            }
            case DELETE -> transfer.setDeleted(false);
            case RESTORE -> transfer.setDeleted(true);
        }

        transferRepository.saveAndFlush(transfer);

        if (transfer.isDeleted()) {
            transferOutbox.transferDeleted(transfer);
        } else {
            transferOutbox.transferChanged(transfer);
        }
    }
}
