package com.emmanuelfinance.transaction.transfer;

import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import com.emmanuelfinance.transaction.transaction.dtos.IdempotencyDataDTO;
import com.emmanuelfinance.transaction.transfer.outbox.TransferOutbox;
import com.emmanuelfinance.transaction.transaction.services.IdempotencyService;
import com.emmanuelfinance.transaction.transfer.dtos.CreateTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.UpdateTransferDTO;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferDomainException;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferErrorCode;
import com.emmanuelfinance.transaction.transfer.services.TransferEventsService;
import com.emmanuelfinance.transaction.transfer.services.TransferService;
import com.emmanuelfinance.transaction.transfer.services.TransferValidatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private TransferEventsService transferEventsService;

    @Mock
    private TransferSelector transferSelector;

    @Mock
    private TransferOutbox transferOutbox;

    @Mock
    private com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator accountOwnershipValidator;

    @Mock
    private com.emmanuelfinance.shared.modules.account.AccountClient accountClient;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        // as regras são testadas à parte; aqui o validator de verdade só precisa deixar passar ou barrar
        TransferValidatorService validator = new TransferValidatorService(accountOwnershipValidator, accountClient);
        transferService = new TransferService(transferRepository, securityUtils, validator, idempotencyService,
                Mappers.getMapper(TransferMapper.class), transferEventsService, transferSelector, transferOutbox);
    }

    private void stubBalance(UUID accountId, String balance) {
        when(accountClient.getAccountSummaryInternal(accountId)).thenReturn(
                new com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO(
                        accountId, "Conta", com.emmanuelfinance.shared.enums.BanksEnum.C6_BANK,
                        new BigDecimal(balance), false));
    }

    @Test
    void shouldCreateAPaidTransferAndRegisterTheOutboxEvents() {
        UUID userId = UUID.randomUUID();
        CreateTransferDTO data = new CreateTransferDTO(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), "Reserva");
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        stubBalance(data.sourceAccountId(), "100.00");
        when(idempotencyService.generateKey(any(IdempotencyDataDTO.class))).thenReturn("chave");
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // o mapper de verdade consultaria o resumo das contas, que aqui não importa
        TransferMapper mapper = mock(TransferMapper.class);
        when(mapper.toEntity(data)).thenReturn(Mappers.getMapper(TransferMapper.class).toEntity(data));
        TransferService service = new TransferService(transferRepository, securityUtils,
                new TransferValidatorService(accountOwnershipValidator, accountClient), idempotencyService, mapper,
                transferEventsService, transferSelector, transferOutbox);

        service.create(data);

        verify(idempotencyService).validateAndLock("chave");
        verify(transferRepository).save(argThat(transfer -> transfer.getStatus() == StatusTransactionEnum.PAID
                && userId.equals(transfer.getUserId()) && "chave".equals(transfer.getIdempotencyKey())));
        verify(transferOutbox).transferChanged(any(Transfer.class));
        verify(transferEventsService).publishCreatedIfPaid(any(Transfer.class));
    }

    @Test
    void shouldNotSaveWhenTheSourceHasNoBalance() {
        CreateTransferDTO data = new CreateTransferDTO(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), null);
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        stubBalance(data.sourceAccountId(), "10.00");

        assertThrows(TransferDomainException.class, () -> transferService.create(data));

        verifyNoInteractions(transferRepository, transferOutbox, transferEventsService);
    }

    @Test
    void shouldRegisterTheOldValuesWhenUpdating() {
        Transfer transfer = TransferTestData.paidTransfer();
        UUID source = transfer.getSourceAccountId();
        UUID destination = transfer.getDestinationAccountId();
        TransferMapper mapper = mock(TransferMapper.class);
        TransferService service = new TransferService(transferRepository, securityUtils,
                new TransferValidatorService(accountOwnershipValidator, accountClient), idempotencyService, mapper,
                transferEventsService, transferSelector, transferOutbox);
        UpdateTransferDTO data = new UpdateTransferDTO(null, null, new BigDecimal("150.00"), null);
        when(transferSelector.getTransferById(transfer.getId())).thenReturn(transfer);
        stubBalance(source, "50.00");
        doAnswer(invocation -> {
            transfer.setAmount(data.amount());
            return null;
        }).when(mapper).updateEntityFromDTO(data, transfer);
        when(transferRepository.saveAndFlush(transfer)).thenReturn(transfer);

        service.update(transfer.getId(), data);

        verify(transferOutbox).transferChanged(transfer);
        verify(transferEventsService).publishUpdatedIfPaid(transfer, source, destination, new BigDecimal("100.00"));
    }

    @Test
    void shouldDeleteAndRegisterTheDeletion() {
        Transfer transfer = TransferTestData.paidTransfer();
        when(transferSelector.getTransferById(transfer.getId())).thenReturn(transfer);
        stubBalance(transfer.getDestinationAccountId(), "100.00");

        transferService.delete(transfer.getId());

        verify(transferRepository).delete(transfer);
        verify(transferOutbox).transferDeleted(transfer);
        verify(transferEventsService).publishDeletedIfPaid(transfer);
    }

    @Test
    void shouldRestoreADeletedTransfer() {
        Transfer transfer = TransferTestData.paidTransfer();
        transfer.setDeleted(true);
        when(transferSelector.getTransferByIdIncludingDeleted(transfer.getId())).thenReturn(transfer);
        stubBalance(transfer.getSourceAccountId(), "100.00");

        transferService.restore(transfer.getId());

        assertFalse(transfer.isDeleted());
        verify(transferOutbox).transferChanged(transfer);
        verify(transferEventsService).publishRestoredIfPaid(transfer);
    }

    @Test
    void shouldNotRestoreAnActiveTransfer() {
        Transfer transfer = TransferTestData.paidTransfer();
        when(transferSelector.getTransferByIdIncludingDeleted(transfer.getId())).thenReturn(transfer);

        TransferDomainException exception = assertThrows(TransferDomainException.class,
                () -> transferService.restore(transfer.getId()));

        assertEquals(TransferErrorCode.RESTORE_TRANSFER_NOT_DELETED.getMessage(), exception.getMessage());
        verifyNoInteractions(transferRepository, transferOutbox);
    }
}
