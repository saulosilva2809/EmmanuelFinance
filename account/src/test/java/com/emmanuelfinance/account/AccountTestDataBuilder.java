package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.enums.BanksEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountTestDataBuilder {

    public static CreateAccountDTO createAccountDTO() {
        return new CreateAccountDTO(
                "Conta C6 BANK",
                TypeEnum.CHECKING,
                BanksEnum.C6_BANK,
                new BigDecimal(5000)

        );
    }

    public static Account accountEntity(CreateAccountDTO inputDto, UUID userId, boolean isDeleted) {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUserId(userId);
        account.setName(inputDto.name());
        account.setType(inputDto.type());
        account.setBank(inputDto.bank());
        account.setInitialBalance(inputDto.initialBalance());
        account.setCurrentBalance(inputDto.initialBalance());
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(null);
        account.setDeleted(isDeleted);
        return account;
    }
}