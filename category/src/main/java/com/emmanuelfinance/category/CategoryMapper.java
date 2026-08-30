package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CreateCategoryDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.category.dto.UpdateCategoryDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    public abstract Category toEntity(CreateCategoryDTO data);

    public abstract ResponseCategoryDTO toResponseDTO(Category entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public abstract void updateCategoryFromDTO(UpdateCategoryDTO data, @MappingTarget Category entity);
}
