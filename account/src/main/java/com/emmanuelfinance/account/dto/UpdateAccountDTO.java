package com.emmanuelfinance.account.dto;

import com.emmanuelfinance.account.enums.TypeEnum;

public record UpdateAccountDTO(
        String name,
        TypeEnum type
) {}
