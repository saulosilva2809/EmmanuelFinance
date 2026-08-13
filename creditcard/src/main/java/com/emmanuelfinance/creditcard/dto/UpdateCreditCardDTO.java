package com.emmanuelfinance.creditcard.dto;

import com.emmanuelfinance.creditcard.enums.BanksEnum;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCreditCardDTO(
        UUID accountId,
        String name,
        BanksEnum bank,
        BigDecimal creditLimit,
        Integer closingDay,
        Integer dueDay
) {}