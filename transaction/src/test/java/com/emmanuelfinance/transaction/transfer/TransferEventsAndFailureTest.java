package com.emmanuelfinance.transaction.transfer;

import com.emmanuelfinance.shared.enums.FailedOperationEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transfer.kafka.dto.TransferFailedEvent;
import com.emmanuelfinance.transaction.transfer.outbox.TransferOutbox;
import com.emmanuelfinance.transaction.transfer.kafka.TransferListener;
import com.emmanuelfinance.transaction.transfer.services.TransferEventsService;
import com.emmanuelfinance.transaction.transfer.services.TransferFailureService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferEventsAndFailureTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferOutbox transferOutbox;

    @InjectMocks
    private TransferEventsService transferEventsService;

    private final Transfer transfer = TransferTestData.paidTransfer();

    private void compensate(TransferFailedEvent event) {
        when(transferRepository.findById(transfer.getId())).thenReturn(Optional.of(transfer));

        new TransferFailureService(transferRepository, transferOutbox).compensate(event);
    }

    private TransferFailedEvent failed(FailedOperationEnum operation) {
        return new TransferFailedEvent(transfer.getId(), operation, null, null, null);
    }

    @Test
    void shouldRegisterTheEventsOfAPaidTransfer() {
        transferEventsService.publishCreatedIfPaid(transfer);
        transferEventsService.publishDeletedIfPaid(transfer);
        transferEventsService.publishRestoredIfPaid(transfer);

        verify(transferOutbox).transferCreatedEvent(argThat(event -> transfer.getId().equals(event.transferId())
                && event.eventId() != null));
        verify(transferOutbox).transferDeletedEvent(any());
        verify(transferOutbox).transferRestoredEvent(any());
    }

    @Test
    void shouldNotWarnTheAccountAboutAFailedTransfer() {
        transfer.setStatus(StatusTransactionEnum.FAILED);

        transferEventsService.publishCreatedIfPaid(transfer);
        transferEventsService.publishDeletedIfPaid(transfer);

        verifyNoInteractions(transferOutbox);
    }

    @Test
    void shouldOnlyWarnAboutAnUpdateThatChangesTheBalance() {
        transferEventsService.publishUpdatedIfPaid(transfer, transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(), new BigDecimal("100"));

        verifyNoInteractions(transferOutbox);

        transferEventsService.publishUpdatedIfPaid(transfer, transfer.getSourceAccountId(),
                transfer.getDestinationAccountId(), BigDecimal.ONE);

        verify(transferOutbox).transferUpdatedEvent(argThat(event -> BigDecimal.ONE.equals(event.oldAmount())));
    }

    @Test
    void shouldMarkAFailedCreation() {
        compensate(failed(FailedOperationEnum.CREATE));

        assertEquals(StatusTransactionEnum.FAILED, transfer.getStatus());
        verify(transferRepository).saveAndFlush(transfer);
        verify(transferOutbox).transferChanged(transfer);
    }

    @Test
    void shouldGoBackToTheOldValuesAfterAFailedUpdate() {
        UUID source = UUID.randomUUID();
        UUID destination = UUID.randomUUID();

        compensate(new TransferFailedEvent(
                transfer.getId(), FailedOperationEnum.UPDATE, source, destination, BigDecimal.ONE));

        assertEquals(source, transfer.getSourceAccountId());
        assertEquals(destination, transfer.getDestinationAccountId());
        assertEquals(BigDecimal.ONE, transfer.getAmount());
    }

    @Test
    void shouldUndoFailedDeletionAndRestore() {
        transfer.setDeleted(true);
        compensate(failed(FailedOperationEnum.DELETE));
        assertFalse(transfer.isDeleted());

        compensate(failed(FailedOperationEnum.RESTORE));
        assertTrue(transfer.isDeleted());
        verify(transferOutbox).transferDeleted(transfer);
    }

    @Test
    void shouldDelegateTheFailureFromTheListener() {
        TransferFailureService service = mock(TransferFailureService.class);
        TransferFailedEvent event = failed(FailedOperationEnum.CREATE);

        new TransferListener(service).handleTransferFailed(event);

        verify(service).compensate(event);
    }
}
