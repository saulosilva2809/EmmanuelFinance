package com.emmanuelfinance.transaction.transfer.services;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.transaction.dtos.IdempotencyDataDTO;
import com.emmanuelfinance.transaction.transfer.outbox.TransferOutbox;
import com.emmanuelfinance.transaction.transaction.services.IdempotencyService;
import com.emmanuelfinance.transaction.transfer.Transfer;
import com.emmanuelfinance.transaction.transfer.TransferMapper;
import com.emmanuelfinance.transaction.transfer.TransferRepository;
import com.emmanuelfinance.transaction.transfer.TransferSelector;
import com.emmanuelfinance.transaction.transfer.dtos.CreateTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.ResponseTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.UpdateTransferDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final SecurityUtils securityUtils;
    private final TransferValidatorService transferValidatorService;
    private final IdempotencyService idempotencyService;
    private final TransferMapper transferMapper;
    private final TransferEventsService transferEventsService;
    private final TransferSelector transferSelector;
    private final TransferOutbox transferOutbox;

    @Transactional
    public ResponseTransferDTO create(CreateTransferDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        transferValidatorService.create.validate(data);

        String idempotencyKey = idempotencyService.generateKey(IdempotencyDataDTO.builder()
                .userId(userId)
                .accountId(data.sourceAccountId())
                .destinationAccountId(data.destinationAccountId())
                .amount(data.amount())
                .description(data.description())
                .build());

        idempotencyService.validateAndLock(idempotencyKey);

        Transfer transfer = transferMapper.toEntity(data);
        transfer.setUserId(userId);
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setStatus(StatusTransactionEnum.PAID);

        // o postgres grava agora; o mongo e o account são avisados pelo outbox depois do commit
        Transfer savedTransfer = transferRepository.save(transfer);
        transferOutbox.transferChanged(savedTransfer);
        transferEventsService.publishCreatedIfPaid(savedTransfer);

        return transferMapper.toResponseDTO(savedTransfer);
    }

    @Transactional
    public ResponseTransferDTO update(UUID transferId, UpdateTransferDTO data) {
        Transfer transfer = transferSelector.getTransferById(transferId);

        // guarda os valores de antes, porque o aviso de atualização leva o antes e o depois
        UUID oldSourceId = transfer.getSourceAccountId();
        UUID oldDestinationId = transfer.getDestinationAccountId();
        BigDecimal oldAmount = transfer.getAmount();

        transferValidatorService.update.validate(transfer, data);

        transferMapper.updateEntityFromDTO(data, transfer);
        Transfer updatedTransfer = transferRepository.saveAndFlush(transfer);

        transferOutbox.transferChanged(updatedTransfer);
        transferEventsService.publishUpdatedIfPaid(updatedTransfer, oldSourceId, oldDestinationId, oldAmount);

        return transferMapper.toResponseDTO(updatedTransfer);
    }

    @Transactional
    public void delete(UUID transferId) {
        Transfer transfer = transferSelector.getTransferById(transferId);

        transferValidatorService.delete.validate(transfer);

        transferRepository.delete(transfer);
        transferOutbox.transferDeleted(transfer);
        transferEventsService.publishDeletedIfPaid(transfer);
    }

    @Transactional
    public void restore(UUID transferId) {
        Transfer transfer = transferSelector.getTransferByIdIncludingDeleted(transferId);

        transferValidatorService.restore.validate(transfer);

        transfer.setDeleted(false);
        transferRepository.saveAndFlush(transfer);
        transferOutbox.transferChanged(transfer);
        transferEventsService.publishRestoredIfPaid(transfer);
    }
}
