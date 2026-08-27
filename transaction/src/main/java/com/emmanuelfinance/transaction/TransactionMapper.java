package com.emmanuelfinance.transaction;

import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.category.CategoryClientCacheService;
import com.emmanuelfinance.shared.modules.category.dtos.CategorySummaryDTO;
import com.emmanuelfinance.shared.modules.creditcard.CreditCardClientCacheService;
import com.emmanuelfinance.shared.modules.creditcard.dto.CreditCardSummaryDTO;
import com.emmanuelfinance.transaction.dtos.CreateTransactionDTO;
import com.emmanuelfinance.transaction.dtos.ResponseTransactionDTO;
import com.emmanuelfinance.transaction.dtos.UpdateTransactionDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class TransactionMapper {

    @Autowired
    protected AccountClientCacheService accountClientCacheService;

    @Autowired
    protected CreditCardClientCacheService creditCardClientCacheService;

    @Autowired
    protected CategoryClientCacheService categoryClientCacheService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    public abstract Transaction toEntity(CreateTransactionDTO data);

    @Mapping(target = "accountSummary", source = "accountId", qualifiedByName = "mapAccount")
    @Mapping(target = "creditCardSummary", source = "creditCardId", qualifiedByName = "mapCreditCard")
    @Mapping(target = "categorySummary", source = "categoryId", qualifiedByName = "mapCategory")
    public abstract ResponseTransactionDTO toResponseDTO(Transaction entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    public abstract void updateEntityFromDTO(UpdateTransactionDTO data, @MappingTarget Transaction entity);

    @Named("mapAccount")
    protected AccountSummaryDTO mapAccount(UUID accountId) {
        return accountId != null ? accountClientCacheService.getAccountSummaryById(accountId) : null;
    }

    @Named("mapCreditCard")
    protected CreditCardSummaryDTO mapCreditCard(UUID cardId) {
        return cardId != null ? creditCardClientCacheService.getCreditCardSummaryDTO(cardId) : null;
    }

    @Named("mapCategory")
    protected CategorySummaryDTO mapCategory(UUID categoryId) {
        return categoryId != null ? categoryClientCacheService.getCategorySummaryDTO(categoryId) : null;
    }
}