package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.CreateCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.ResponseCreditCardDTO;
import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.modules.account.AccountClientCacheService;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class CreditCardMapper {

    @Autowired
    protected AccountClientCacheService accountClientCacheService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "availableLimit", ignore = true)
    @Mapping(target = "creditLimit", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    public abstract CreditCard toEntity(CreateCreditCardDTO data);

    @Mapping(target = "account", source = "accountId", qualifiedByName = "mapAccount")
    public abstract ResponseCreditCardDTO toResponseDTO(CreditCard entity);

    @Named("mapAccount")
    protected AccountSummaryDTO mapAccount(UUID accountId) {
        return accountId != null ? accountClientCacheService.getAccountSummaryById(accountId) : null;
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateCreditCardFromDTO(UpdateCreditCardDTO data, @MappingTarget CreditCard entity);
}
