package com.emmanuelfinance.transaction.transfer.services;

import com.emmanuelfinance.shared.modules.account.AccountClient;
import com.emmanuelfinance.shared.modules.account.AccountOwnershipValidator;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.transaction.transfer.Transfer;
import com.emmanuelfinance.transaction.transfer.dtos.CreateTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.UpdateTransferDTO;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferDomainException;
import com.emmanuelfinance.transaction.transfer.exceptions.TransferErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferValidatorService {

    private final AccountOwnershipValidator accountOwnershipValidator;
    private final AccountClient accountClient;

    public final CreateMethodValidations create = new CreateMethodValidations();
    public final UpdateMethodValidations update = new UpdateMethodValidations();
    public final DeleteMethodValidations delete = new DeleteMethodValidations();
    public final RestoreMethodValidations restore = new RestoreMethodValidations();

    // origem e destino precisam ser contas diferentes
    private void validateAccounts(UUID sourceAccountId, UUID destinationAccountId) {
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new TransferDomainException(TransferErrorCode.SAME_ACCOUNTS);
        }
    }

    private void validatePositiveValue(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferDomainException(TransferErrorCode.VALUE_MUST_BE_POSITIVE);
        }
    }

    // saldo lido direto do account, porque o valor em cache pode estar atrasado
    private BigDecimal balanceOf(UUID accountId) {
        return accountClient.getAccountSummaryInternal(accountId).currentBalance();
    }

    private void validateBalance(BigDecimal available, BigDecimal amount) {
        if (amount.compareTo(available) > 0) {
            throw new TransferDomainException(TransferErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    public class CreateMethodValidations {
        public void validate(CreateTransferDTO data) {
            validatePositiveValue(data.amount());
            validateAccounts(data.sourceAccountId(), data.destinationAccountId());
            accountOwnershipValidator.validate(data.sourceAccountId());
            accountOwnershipValidator.validate(data.destinationAccountId());
            validateBalance(balanceOf(data.sourceAccountId()), data.amount());
        }
    }

    public class UpdateMethodValidations {
        public void validate(Transfer entity, UpdateTransferDTO data) {
            // a que falhou nunca saiu do papel, então não há o que alterar
            if (entity.getStatus() == StatusTransactionEnum.FAILED) {
                throw new TransferDomainException(TransferErrorCode.FAILED_TRANSFER_CANNOT_BE_CHANGED);
            }

            // o que não veio no pedido continua como está
            UUID source = data.sourceAccountId() != null ? data.sourceAccountId() : entity.getSourceAccountId();
            UUID destination = data.destinationAccountId() != null
                    ? data.destinationAccountId()
                    : entity.getDestinationAccountId();
            BigDecimal amount = data.amount() != null ? data.amount() : entity.getAmount();

            validatePositiveValue(amount);
            validateAccounts(source, destination);

            if (!source.equals(entity.getSourceAccountId())) {
                accountOwnershipValidator.validate(source);
            }
            if (!destination.equals(entity.getDestinationAccountId())) {
                accountOwnershipValidator.validate(destination);
            }

            boolean balanceUnchanged = source.equals(entity.getSourceAccountId())
                    && destination.equals(entity.getDestinationAccountId())
                    && amount.compareTo(entity.getAmount()) == 0;
            if (balanceUnchanged) {
                return;
            }

            // desfazendo a antiga, a origem antiga recebe o valor de volta e o destino antigo perde
            BigDecimal available = balanceOf(source);
            if (source.equals(entity.getSourceAccountId())) {
                available = available.add(entity.getAmount());
            } else if (source.equals(entity.getDestinationAccountId())) {
                available = available.subtract(entity.getAmount());
            }
            validateBalance(available, amount);
        }
    }

    public class DeleteMethodValidations {
        public void validate(Transfer entity) {
            // excluir tira o dinheiro de volta do destino, que precisa ainda ter o valor
            if (entity.getStatus() == StatusTransactionEnum.PAID) {
                validateBalance(balanceOf(entity.getDestinationAccountId()), entity.getAmount());
            }
        }
    }

    public class RestoreMethodValidations {
        public void validate(Transfer entity) {
            if (!entity.isDeleted()) {
                throw new TransferDomainException(TransferErrorCode.RESTORE_TRANSFER_NOT_DELETED);
            }

            if (entity.getStatus() == StatusTransactionEnum.PAID) {
                validateBalance(balanceOf(entity.getSourceAccountId()), entity.getAmount());
            }
        }
    }
}
