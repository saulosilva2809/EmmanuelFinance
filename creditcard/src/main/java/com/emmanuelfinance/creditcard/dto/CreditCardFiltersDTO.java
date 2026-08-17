package com.emmanuelfinance.creditcard.dto;

import com.emmanuelfinance.shared.enums.BanksEnum;

import java.util.UUID;

public record CreditCardFiltersDTO (
        UUID accountId,
        String name,
        BanksEnum bank
) {}