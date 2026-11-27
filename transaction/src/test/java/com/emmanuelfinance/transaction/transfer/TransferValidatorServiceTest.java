package com.emmanuelfinance.transaction.transfer;

import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.AccountClient;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.emmanuelfinance.shared.modules.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.transfer.dtos.CreateTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.UpdateTransferDTO;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferDomainException;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferErrorCode;
import com.emmanuelfinance.transaction.transfer.services.TransferValidatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferValidatorServiceTest {

    @Mock
    private AccountOwnershipValidator accountOwnershipValidator;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private TransferValidatorService validator;

    private final Transfer transfer = TransferTestData.paidTransfer();

    private void stubBalance(UUID accountId, String balance) {
        when(accountClient.getAccountSummaryInternal(accountId)).thenReturn(new AccountSummaryInternalDTO(
                accountId, "Conta", BanksEnum.C6_BANK, new BigDecimal(balance), false));
    }

    private CreateTransferDTO create(UUID source, UUID destination, String amount) {
        return new CreateTransferDTO(source, destination, new BigDecimal(amount), null);
    }

    private UpdateTransferDTO newAmount(String amount) {
        return new UpdateTransferDTO(null, null, new BigDecimal(amount), null);
    }

    private void assertError(TransferErrorCode expected, Executable executable) {
        assertEquals(expected.getMessage(), assertThrows(TransferDomainException.class, executable).getMessage());
    }

    @Test
    void shouldRefuseZeroAndNegativeAmounts() {
        for (String amount : new String[]{"0", "-1"}) {
            assertError(TransferErrorCode.VALUE_MUST_BE_POSITIVE,
                    () -> validator.create.validate(create(UUID.randomUUID(), UUID.randomUUID(), amount)));
        }
        verifyNoInteractions(accountClient);
    }

    @Test
    void shouldRefuseTheSameAccount() {
        UUID account = UUID.randomUUID();

        assertError(TransferErrorCode.SAME_ACCOUNTS, () -> validator.create.validate(create(account, account, "10")));
    }

    @Test
    void shouldCheckTheOwnershipBeforeTheBalance() {
        CreateTransferDTO data = create(UUID.randomUUID(), UUID.randomUUID(), "10");
        doThrow(new AccountNotFound()).when(accountOwnershipValidator).validate(data.sourceAccountId());

        assertThrows(AccountNotFound.class, () -> validator.create.validate(data));

        verifyNoInteractions(accountClient);
    }

    @Test
    void shouldRefuseWhenTheBalanceIsNotEnough() {
        CreateTransferDTO data = create(UUID.randomUUID(), UUID.randomUUID(), "10.01");
        stubBalance(data.sourceAccountId(), "10.00");

        assertError(TransferErrorCode.INSUFFICIENT_BALANCE, () -> validator.create.validate(data));
    }

    @Test
    void shouldNeedBalanceOnlyForTheDifferenceOnUpdate() {
        stubBalance(transfer.getSourceAccountId(), "10.00");

        assertDoesNotThrow(() -> validator.update.validate(transfer, newAmount("110.00")));
        assertError(TransferErrorCode.INSUFFICIENT_BALANCE,
                () -> validator.update.validate(transfer, newAmount("110.01")));
    }

    @Test
    void shouldNotAskTheBalanceWhenOnlyTheDescriptionChanges() {
        validator.update.validate(transfer, new UpdateTransferDTO(null, null, null, "Outra"));

        verifyNoInteractions(accountClient, accountOwnershipValidator);
    }

    @Test
    void shouldRefuseChangingAFailedTransfer() {
        transfer.setStatus(StatusTransactionEnum.FAILED);

        assertError(TransferErrorCode.FAILED_TRANSFER_CANNOT_BE_CHANGED,
                () -> validator.update.validate(transfer, newAmount("1")));
    }

    @Test
    void shouldNeedBalanceInTheDestinationToDelete() {
        stubBalance(transfer.getDestinationAccountId(), "99.99");

        assertError(TransferErrorCode.INSUFFICIENT_BALANCE, () -> validator.delete.validate(transfer));
    }

    @Test
    void shouldNeedBalanceInTheSourceToRestore() {
        transfer.setDeleted(true);
        stubBalance(transfer.getSourceAccountId(), "99.99");

        assertError(TransferErrorCode.INSUFFICIENT_BALANCE, () -> validator.restore.validate(transfer));
    }
}
