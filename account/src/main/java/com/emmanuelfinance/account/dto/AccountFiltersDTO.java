package com.emmanuelfinance.account.dto;

import com.emmanuelfinance.account.enums.TypeEnum;

public record AccountFiltersDTO(
        String name,
        TypeEnum type
) {}
