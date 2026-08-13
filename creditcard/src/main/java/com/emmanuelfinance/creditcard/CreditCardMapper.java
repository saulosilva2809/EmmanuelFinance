package com.emmanuelfinance.creditcard;

import com.emmanuelfinance.creditcard.dto.UpdateCreditCardDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCreditCardFromDTO(UpdateCreditCardDTO data, @MappingTarget CreditCard entity);
}
