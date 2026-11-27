package com.emmanuelfinance.transaction.transfer;

import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.transaction.dtos.UpdateTransactionDTO;
import com.emmanuelfinance.transaction.transfer.dtos.CreateTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.ResponseTransferDTO;
import com.emmanuelfinance.transaction.transfer.dtos.UpdateTransferDTO;
import com.emmanuelfinance.transaction.transfer.query.TransferDocument;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class TransferMapper {

    @Autowired
    protected AccountClientCacheService accountClientCacheService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    public abstract Transfer toEntity(CreateTransferDTO data);

    public abstract TransferDocument toDocument(Transfer entity);

    @Mapping(target = "sourceAccount", source = "sourceAccountId", qualifiedByName = "mapAccount")
    @Mapping(target = "destinationAccount", source = "destinationAccountId", qualifiedByName = "mapAccount")
    public abstract ResponseTransferDTO toResponseDTO(Transfer entity);

    @Mapping(target = "sourceAccount", source = "sourceAccountId", qualifiedByName = "mapAccount")
    @Mapping(target = "destinationAccount", source = "destinationAccountId", qualifiedByName = "mapAccount")
    public abstract ResponseTransferDTO toResponseDTO(TransferDocument document);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    public abstract void updateEntityFromDTO(UpdateTransferDTO data, @MappingTarget Transfer entity);

    @Named("mapAccount")
    protected AccountSummaryDTO mapAccount(UUID accountId) {
        return accountId != null ? accountClientCacheService.getAccountSummaryById(accountId) : null;
    }
}
