package com.emmanuelfinance.transaction.transfer;

import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferDomainException;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransferSelector {

    private final TransferRepository transferRepository;
    private final SecurityUtils securityUtils;

    public Transfer getTransferById(UUID transferId) {
        UUID userId = securityUtils.getCurrentUserId();

        Transfer transfer = transferRepository.findByIdAndUserIdAndDeletedFalse(transferId, userId)
                .orElseThrow(() -> new TransferDomainException(TransferErrorCode.TRANSFER_NOT_FOUND));

        return transfer;
    }

    public Transfer getTransferByIdIncludingDeleted(UUID transferId) {
        UUID userId = securityUtils.getCurrentUserId();

        return transferRepository.findByIdAndUserId(transferId, userId)
                .orElseThrow(() -> new TransferDomainException(TransferErrorCode.TRANSFER_NOT_FOUND));
    }
}
