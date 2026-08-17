package com.emmanuelfinance.account.dto;

import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.shared.enums.BanksEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateAccountDTO (
        @NotBlank(message = "The name is required")
        String name,

        @NotNull(message = "The type is required")
        TypeEnum type,

        @NotNull(message = "The bank is required")
        BanksEnum bank,

        @NotNull(message = "The initial balance is required")
        @PositiveOrZero(message = "Initial balance must be zero or positive")
        BigDecimal initialBalance
) {}
