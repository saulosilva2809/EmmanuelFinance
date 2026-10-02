package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;


@Mapper(componentModel = "spring")
public abstract class AccountMapper {

    @Autowired
    protected com.emmanuelfinance.shared.modules.user.UserClientCacheService userClientCacheService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    public abstract Account toEntity(CreateAccountDTO data);

    public abstract ResponseAccountDTO toResponseDTO(Account entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateAccountFromDTO(UpdateAccountDTO data, @MappingTarget Account entity);
}
