package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountSelector;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountInternalService {

    private final AccountSelector accountSelector;

    public AccountSummaryDTO getAccountSummary(UUID id) {
        Account account = accountSelector.getAccountByIdIncludingDeleted(id);

        return new AccountSummaryDTO(account.getId(), account.getName(), account.isDeleted());
    }

    public AccountSummaryInternalDTO getAccountSummaryInternal(UUID id) {
        Account account = accountSelector.getAccountByIdIncludingDeleted(id);

        return new AccountSummaryInternalDTO(
                account.getId(),
                account.getName(),
                account.getBank(),
                account.isDeleted()
        );
    }
}
