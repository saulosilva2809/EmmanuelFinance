package com.emmanuelfinance.category.dto;

import com.emmanuelfinance.category.enums.TypeEnum;

import java.util.UUID;

public record CategoryFIltersDTO (
        UUID accountId,
        String name,
        TypeEnum type
) {}
